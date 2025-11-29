package com.simplyawakeremake.usecases

import android.util.Log
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SyncUserTracksAfterLoginUseCase(
    private val userRepository: UserRepository,
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
    private val syncService: UserTrackSyncService,
) {

    private val TAG = "SyncTracksAfterLoginUC"

    suspend operator fun invoke(): UserTrackSyncResult = withContext(Dispatchers.IO) {
        val user = userRepository.currentUser.first()
        val firebaseUid = user.firebaseUid ?: return@withContext UserTrackSyncResult.Failure(
            IllegalStateException("Cannot sync: current user is a guest (no firebaseUid)")
        )

        val localUserId = user.id
        val localTracks = local.getAllForUser(localUserId)
        val remoteTracks = remote.fetchAllForUser(firebaseUid)

        return@withContext when {
            remoteTracks.isEmpty() && localTracks.isNotEmpty() -> {
                Log.i(TAG, "Login sync: remote empty, local has data -> sync UP")
                syncService.syncUpFromLocalToRemote(localTracks)
                val updatedRemoteTracks = remote.fetchAllForUser(firebaseUid)
                Log.i(TAG, "Login sync: syncing DOWN from freshly updated remote snapshot")
                syncService.syncDownFromRemoteToLocal(updatedRemoteTracks)
            }

            else -> {
                Log.i(TAG, "Login sync: remote has data (or both empty) -> sync DOWN")
                syncService.syncDownFromRemoteToLocal(remoteTracks)
            }
        }
    }

}
