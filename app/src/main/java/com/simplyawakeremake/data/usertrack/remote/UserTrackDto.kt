package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.UserTrack

data class UserTrackDto(
    val userId: String = "",
    val trackId: String = "",
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val updatedAt: Long = 0L,
)
fun UserTrackDto.toDomain(): UserTrack =
    UserTrack(
        userId = userId,
        trackId = trackId,
        isFavorite = favorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        updatedAt = updatedAt
    )

fun UserTrack.toDto(): UserTrackDto =
    UserTrackDto(
        userId = userId,
        trackId = trackId,
        favorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        updatedAt = updatedAt
    )