package com.simplyawakeremake.data.usertrack.local

import androidx.room.Entity
import com.simplyawakeremake.data.usertrack.UserTrack

@Entity(
    tableName = "user_tracks",
    primaryKeys = ["userId", "trackId"]
)
data class UserTrackEntity(
    val userId: String,
    val trackId: String,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val updatedAt: Long = 0L
)

fun UserTrackEntity.toDomain(): UserTrack =
    UserTrack(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        updatedAt = updatedAt
    )

fun UserTrack.toEntity(): UserTrackEntity =
    UserTrackEntity(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        updatedAt = updatedAt
    )