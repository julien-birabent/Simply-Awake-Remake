package com.simplyawakeremake.extensions

/**
 * Examples:
 * - "10:00"     -> "10m"
 * - "00:45"     -> "45s"
 * - "01:05:00"  -> "1h5m"
 * - "00:00"     -> "0s"
 */
fun String.toCompactDurationLabel(): String {
    val parts = split(":").mapNotNull { it.toIntOrNull() }

    val (hours, minutes, seconds) = when (parts.size) {
        3 -> Triple(parts[0], parts[1], parts[2])
        2 -> Triple(0, parts[0], parts[1])
        1 -> Triple(0, parts[0], 0)
        else -> return this
    }

    return when {
        hours > 0 -> { if (minutes > 0) "${hours} h${minutes} min" else "${hours}h" }

        minutes > 0 -> { "$minutes min" }

        seconds > 0 -> { "$seconds s" }

        else -> "0s"
    }
}
