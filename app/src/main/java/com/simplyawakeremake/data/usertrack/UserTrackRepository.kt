package com.simplyawakeremake.data.usertrack

import android.util.Log
import com.simplyawakeremake.data.user.User
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import com.simplyawakeremake.now
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class UserTrackRepository(
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
    private val userRepository: UserRepository
) {

    private val TAG = "UserTrackRepository"

    private fun UserTrack.withUpdatedTimestamp(): UserTrack =
        copy(updatedAt = now())

    fun observeAll(): Flow<List<UserTrack>> =
        userRepository.currentUser.flatMapLatest { user -> local.observeAll(user.id) }

    fun observeTrack(trackId: String): Flow<UserTrack?> =
        userRepository.currentUser.flatMapLatest { user -> local.observeOne(user.id, trackId) }

    private fun defaultUserTrack(userId: String, trackId: String): UserTrack =
        UserTrack(
            userId = userId,
            trackId = trackId,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = null
        ).withUpdatedTimestamp()

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        val user = userRepository.currentUser.first()
        val userId = user.id

        val current = local.getOne(userId, trackId) ?: defaultUserTrack(userId, trackId)

        val updated = current
            .copy(isFavorite = isFavorite)
            .withUpdatedTimestamp()
        Log.i(TAG, "toggleFavorite: $updated")

        local.upsert(updated)

        syncRemoteIfLoggedIn(
            user = user,
            operationName = "favorite"
        ) {
            user.firebaseUid?.let { remote.upsertUserTrack(it, updated) }
        }
    }

    suspend fun registerPlay(trackId: String, playedAtMillis: Long) {
        val user = userRepository.currentUser.first()
        val userId = user.id

        val current = local.getOne(userId, trackId) ?: defaultUserTrack(userId, trackId)

        val updated = current.copy(
            playCount = current.playCount + 1,
            lastPlayedAt = playedAtMillis,
        ).withUpdatedTimestamp()

        Log.i(TAG, "registerPlay: $updated")

        local.upsert(updated)

        syncRemoteIfLoggedIn(
            user = user,
            operationName = "play"
        ) {

            user.firebaseUid?.let {
                Log.i(
                    TAG,
                    "toggleFavorite remote upsert: remoteUserId=$it, trackId=$trackId, isFavorite=${updated.isFavorite}"
                )
                remote.upsertUserTrack(remoteUserId = it, updated)
            }
        }
    }

    private suspend fun syncRemoteIfLoggedIn(
        user: User,
        operationName: String,
        block: suspend () -> Unit,
    ) {
        if (user.firebaseUid == null) {
            Log.d(TAG, "Guest user (no firebaseUid); skipping remote $operationName sync")
            return
        }

        try {
            block()
        } catch (e: Exception) {
            Log.w(
                TAG,
                "Failed to sync $operationName to Firestore for user=${user.id}, " +
                        "keeping local only",
                e
            )
        }
    }
}
