package com.simplyawakeremake.data.usertrack.remote

import com.simplyawakeremake.data.usertrack.UserTrack

data class UserTrackDto(
    val userId: String = "",
    val trackId: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val updatedAt: Long = 0L,
)
fun UserTrackDto.toDomain(): UserTrack =
    UserTrack(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
    )

fun UserTrack.toDto(): UserTrackDto =
    UserTrackDto(
        userId = userId,
        trackId = trackId,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
    )