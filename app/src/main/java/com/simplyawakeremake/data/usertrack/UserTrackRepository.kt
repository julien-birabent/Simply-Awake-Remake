package com.simplyawakeremake.data.usertrack

import android.util.Log
import com.simplyawakeremake.data.user.User
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.local.toDomain
import com.simplyawakeremake.data.usertrack.local.toEntity
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class UserTrackRepository(
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
    private val userRepository: UserRepository
) {

    private val TAG = "UserTrackRepository"

    fun observeAll(): Flow<List<UserTrack>> =
        userRepository.currentUser
            .flatMapLatest { user -> local.observeAll(user.id) }
            .map { entities -> entities.map { it.toDomain() } }

    fun observeTrack(trackId: String): Flow<UserTrack?> =
        userRepository.currentUser
            .flatMapLatest { user -> local.observeOne(user.id, trackId) }
            .map { entity -> entity?.toDomain() }

    private fun defaultUserTrack(userId: String, trackId: String): UserTrack =
        UserTrack(
            userId = userId,
            trackId = trackId,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = null,
        )

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        val user = userRepository.ensureLocalUserExists()
        val userId = user.id

        val current = local.getOne(userId, trackId)?.toDomain()
            ?: defaultUserTrack(userId, trackId)

        val updated = current.copy(isFavorite = isFavorite)
        Log.i(TAG, "toggleFavorite: $updated")

        local.upsert(updated.toEntity())

        syncRemoteIfLoggedIn(
            user = user,
            operationName = "favorite"
        ) {
            remote.upsertUserTrack(updated)
        }
    }

    suspend fun registerPlay(trackId: String, playedAtMillis: Long) {
        val user = userRepository.ensureLocalUserExists()
        val userId = user.id

        val current = local.getOne(userId, trackId)?.toDomain()
            ?: defaultUserTrack(userId, trackId)

        val updated = current.copy(
            playCount = current.playCount + 1,
            lastPlayedAt = playedAtMillis
        )

        Log.i(TAG, "registerPlay: $updated")

        local.upsert(updated.toEntity())

        syncRemoteIfLoggedIn(
            user = user,
            operationName = "play"
        ) {
            remote.upsertUserTrack(updated)
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
