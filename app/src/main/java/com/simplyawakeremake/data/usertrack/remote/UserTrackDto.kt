package com.simplyawakeremake.data.usertrack.remote

/**
 * Pure remote representation of a user track.
 * This type is only used at the API / Firestore boundary.
 */
data class UserTrackDto(
    val trackId: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val updatedAt: Long = 0L,
)
