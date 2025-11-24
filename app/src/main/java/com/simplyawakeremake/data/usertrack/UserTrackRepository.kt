package com.simplyawakeremake.data.usertrack

import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import com.simplyawakeremake.data.usertrack.remote.UserTrackRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class UserTrackRepository(
    private val userId: String,
    private val local: UserTrackLocalDataSource,
    private val remote: UserTrackRemoteDataSource,
) {

    fun observeAll(): Flow<List<UserTrack>> =
        local.observeAll(userId).map { entities -> entities.map { it.toDomain() } }

    fun observeTrack(trackId: String): Flow<UserTrack?> =
        local.observeOne(userId, trackId).map { entity -> entity?.toDomain() }

    private fun defaultUserTrack(trackId: String): UserTrack =
        UserTrack(
            userId = userId,
            trackId = trackId,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = null,
            updatedAt = 0L
        )

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        val current = local.getOne(userId, trackId)?.toDomain() ?: defaultUserTrack(trackId)

        val updated = current.copy(isFavorite = isFavorite)
        local.upsert(updated.toEntity())
        try {
            remote.upsertUserTrack(updated)
        } catch (e: Exception) {
            // Optionally log/track error; don't affect local
        }
    }

    suspend fun registerPlay(trackId: String, playedAtMillis: Long) {
        val current = local.getOne(userId, trackId)?.toDomain() ?: defaultUserTrack(trackId)

        val updated = current.copy(
            playCount = current.playCount + 1,
            lastPlayedAt = playedAtMillis
        )

        local.upsert(updated.toEntity())

        try {
            remote.upsertUserTrack(updated)
        } catch (e: Exception) {
            // Optionally log/track error; don't affect local
        }
    }
}