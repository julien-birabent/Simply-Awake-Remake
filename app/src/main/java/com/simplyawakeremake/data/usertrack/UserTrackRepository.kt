package com.simplyawakeremake.data.usertrack

import android.util.Log
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalCoroutinesApi::class)
class UserTrackRepository(
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
    private val userRepository: UserRepository
) {

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
            updatedAt = 0L
        )

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        val userId = userRepository.requireCurrentUserId()
        val current = local.getOne(userId, trackId)?.toDomain() ?: defaultUserTrack(userId, trackId)

        val updated = current.copy(isFavorite = isFavorite)
        Log.i("UserTrackRepository", "toggleFavorite: $updated")
        local.upsert(updated.toEntity())
        try {
            remote.upsertUserTrack(updated)
        } catch (e: Exception) {
            // Optionally log/track error; don't affect local
        }
    }

    suspend fun registerPlay(trackId: String, playedAtMillis: Long) {
        val userId = userRepository.requireCurrentUserId()
        val current = local.getOne(userId, trackId)?.toDomain()
            ?: defaultUserTrack(userId, trackId)

        val updated = current.copy(
            playCount = current.playCount + 1,
            lastPlayedAt = playedAtMillis
        )
        Log.i("UserTrackRepository", "registerPlay: $updated")

        local.upsert(updated.toEntity())

        try {
            remote.upsertUserTrack(updated)
        } catch (e: Exception) {
            // Optionally log/track error; don't affect local
        }
    }
}