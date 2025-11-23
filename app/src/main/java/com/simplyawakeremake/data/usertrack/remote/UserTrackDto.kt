package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

data class UserTrackDto(
    val trackId: String,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
)

fun UserTrackDto.toEntity(userId: String): UserTrackEntity =
    UserTrackEntity(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt
    )