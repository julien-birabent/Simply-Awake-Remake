package com.simplyawakeremake.data.usertrack


import com.simplyawakeremake.data.usertrack.local.UserTrackEntity
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import kotlinx.coroutines.flow.Flow

class UserTrackRepository(
    private val userId: String,
    private val local: UserTrackLocalDataSource,
) {

    fun observeAll(): Flow<List<UserTrackEntity>> =
        local.observeAll(userId)

    fun observeTrack(trackId: String): Flow<UserTrackEntity?> =
        local.observeOne(userId, trackId)

    suspend fun toggleFavorite(trackId: String, isFavorite: Boolean) {
        val current = local.getOne(userId, trackId)
        val updated = (current ?: UserTrackEntity(
            userId = userId,
            trackId = trackId,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = null
        )).copy(
            isFavorite = isFavorite
        )
        local.upsert(updated)

        // later: sync to Firebase/backend here
    }

    suspend fun registerPlay(trackId: String, playedAtMillis: Long) {
        val current = local.getOne(userId, trackId)
        val updated = (current ?: UserTrackEntity(
            userId = userId,
            trackId = trackId,
            isFavorite = false,
            playCount = 0,
            lastPlayedAt = null
        )).copy(
            playCount = (current?.playCount ?: 0) + 1,
            lastPlayedAt = playedAtMillis
        )
        local.upsert(updated)

        // later: sync to Firebase/backend here
    }
}
