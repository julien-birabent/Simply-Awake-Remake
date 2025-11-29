package com.simplyawakeremake.data.usertrack.sync

import android.util.Log
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.UserTrack
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import kotlinx.coroutines.flow.first

class UserTrackSyncServiceImpl(
    private val userRepository: UserRepository,
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
) : UserTrackSyncService {

    private val TAG = "UserTrackSyncService"

    /**
     * Upload the given [localTracks] snapshot to remote for the current user.
     * Assumes [localTracks] are already filtered for the current local user.
     */
    override suspend fun syncUpFromLocalToRemote(localTracks: List<UserTrack>): UserTrackSyncResult {
        val user = userRepository.currentUser.first()
        val remoteUserId = user.firebaseUid ?: return UserTrackSyncResult.Failure(
            IllegalStateException("Cannot sync up: current user is a guest (no firebaseUid)")
        )

        if (localTracks.isEmpty()) {
            Log.d(TAG, "syncUpFromLocalToRemote: no local user tracks passed, nothing to upload")
            return UserTrackSyncResult.Success(uploadedCount = 0)
        }

        return try {
            Log.i(
                TAG,
                "syncUpFromLocalToRemote: uploading ${localTracks.size} tracks to remoteUserId=$remoteUserId"
            )

            localTracks.forEach { track ->
                remote.upsertUserTrack(
                    remoteUserId = remoteUserId,
                    userTrack = track
                )
            }

            UserTrackSyncResult.Success(uploadedCount = localTracks.size)
        } catch (e: Exception) {
            Log.e(TAG, "syncUpFromLocalToRemote: failed to upload tracks", e)
            UserTrackSyncResult.Failure(e)
        }
    }

    /**
     * Apply the given [remoteTracks] snapshot to the local DB for the current user.
     * Rebinds the [userId] to the current local user before persisting.
     */
    override suspend fun syncDownFromRemoteToLocal(
        remoteTracks: List<UserTrack>
    ): UserTrackSyncResult {
        val user = userRepository.currentUser.first()
        val remoteUserId = user.firebaseUid ?: return UserTrackSyncResult.Failure(
            IllegalStateException("Cannot sync: current user is a guest (no firebaseUid)")
        )
        val localUserId = user.id

        val localTracks = try {
            local.getAllForUser(localUserId)
        } catch (e: Exception) {
            Log.e(TAG, "syncDownFromRemoteToLocal: failed to load local tracks", e)
            return UserTrackSyncResult.Failure(e)
        }

        val localById = localTracks.associateBy { it.trackId }
        val remoteById = remoteTracks.associateBy { it.trackId }

        val allTrackIds = (localById.keys + remoteById.keys).toSet()

        val tracksToUploadRemote = mutableListOf<UserTrack>()
        val tracksToSaveLocal = mutableListOf<UserTrack>()

        for (trackId in allTrackIds) {
            val localTrack = localById[trackId]
            val remoteTrack = remoteById[trackId]

            when {
                localTrack == null && remoteTrack != null -> {
                    Log.d(TAG, "merge[$trackId]: only remote present, will save to local")
                    tracksToSaveLocal += remoteTrack.copy(userId = localUserId)
                }

                localTrack != null && remoteTrack == null -> {
                    Log.d(
                        TAG,
                        "merge[$trackId]: only local present (updatedAt=${localTrack.updatedAt}), will upload to remote"
                    )
                    tracksToUploadRemote += localTrack.copy(userId = remoteUserId)
                }

                localTrack != null && remoteTrack != null -> {
                    when {
                        localTrack.updatedAt > remoteTrack.updatedAt -> {
                            Log.d(TAG, "merge[$trackId]: local is newer, will upload to remote")
                            tracksToUploadRemote += localTrack.copy(userId = remoteUserId)
                        }

                        localTrack.updatedAt < remoteTrack.updatedAt -> {
                            Log.d(TAG, "merge[$trackId]: remote is newer, will save to local")
                            tracksToSaveLocal += remoteTrack.copy(userId = localUserId)
                        }

                        else -> {
                            Log.d(TAG, "merge[$trackId]: equal updatedAt, no-op")
                        }
                    }
                }

                else -> Unit
            }
        }


        Log.i(
            TAG,
            "Two-way sync summary: " +
                    "${tracksToUploadRemote.size} track(s) will be pushed to remote, " +
                    "${tracksToSaveLocal.size} track(s) will be written to local"
        )

        try {
            tracksToUploadRemote.forEach { track ->
                remote.upsertUserTrack(
                    remoteUserId = remoteUserId,
                    userTrack = track
                )
            }
            Log.i(TAG, "Two-way sync: uploaded ${tracksToUploadRemote.size} track(s) to remoteUserId=$remoteUserId")
        } catch (e: Exception) {
            Log.e(TAG, "syncDownFromRemoteToLocal: failed to upload tracks to remote", e)
            return UserTrackSyncResult.Failure(e)
        }

        return try {
            local.upsertAll(tracksToSaveLocal)
            Log.i(
                TAG,
                "syncDownFromRemoteToLocal: saved ${tracksToSaveLocal.size} tracks for localUserId=$localUserId"
            )
            UserTrackSyncResult.Success(
                uploadedCount = tracksToUploadRemote.size,
                downloadedCount = tracksToSaveLocal.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "syncDownFromRemoteToLocal: failed to persist to local DB", e)
            UserTrackSyncResult.Failure(e)
        }
    }

}
