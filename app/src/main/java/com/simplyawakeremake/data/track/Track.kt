package com.simplyawakeremake.data.track

import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

data class Track(
    val id: String,
    val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val duration: String,
    val season: Int,
    val year : Int,

    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
) {
    val ordinal: Int = name.substring(0, 3).removeRange(0, 1).toInt()
    val displayName: String = name.replace(Regex("\\d"), "").trim()
}

fun Track.withUserMeta(userMeta: UserTrackEntity?): Track =
    copy(
        isFavorite = userMeta?.isFavorite ?: false,
        playCount = userMeta?.playCount ?: 0,
        lastPlayedAt = userMeta?.lastPlayedAt
    )
