package com.simplyawakeremake.data.usertrack

data class UserTrack(
    val userId: String,
    val trackId: String,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?,
    val updatedAt: Long = 0L
)