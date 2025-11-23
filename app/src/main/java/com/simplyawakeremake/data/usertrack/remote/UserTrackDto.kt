package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

data class UserTrackDto(
    val trackId: String,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val updatedAt: Long = 0L,
)

fun UserTrackDto.toEntity(userId: String): UserTrackEntity =
    UserTrackEntity(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt
    )

fun UserTrackEntity.toDto(now: Long): UserTrackDto =
    UserTrackDto(
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        updatedAt = now,
    )