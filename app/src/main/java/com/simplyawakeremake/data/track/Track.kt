package com.simplyawakeremake.data.track

import com.simplyawakeremake.data.usertrack.UserTrack
import kotlin.math.max


data class Track(
    val id: String,
    val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val duration: String,
    val season: Int,
    val year: Int,
    val createdAt: Int = 0,

    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
) {
    val ordinal: Int = name.substring(0, 3).removeRange(0, 1).toInt()
    val displayName: String = name.replace(Regex("\\d"), "").trim()
}

fun Track.withUserMeta(meta: UserTrack?): Track =
    if (meta == null) this else copy(
        isFavorite = meta.isFavorite,
        playCount = meta.playCount,
        lastPlayedAt = meta.lastPlayedAt,
    )

fun Track.rawDurationSeconds(): Int {
    return duration.toDurationSeconds()
}

/**
 * Expected format: "mm:ss"
 * - If parsing fails, returns 0 as a safe default.
 */
fun String.toDurationSeconds(): Int {
    val parts = split(":")
    if (parts.size != 2) return 0

    val minutes = parts[0].toIntOrNull() ?: 0
    val seconds = parts[1].toIntOrNull() ?: 0

    val safeMinutes = max(minutes, 0)
    val safeSeconds = max(seconds, 0)

    return safeSeconds + safeMinutes * 60
}

fun Track.categories(): Set<String> {
    return tagString
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
}
