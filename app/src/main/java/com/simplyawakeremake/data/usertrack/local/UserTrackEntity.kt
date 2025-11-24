package com.simplyawakeremake.data.usertrack.local

import androidx.room.Entity

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
)