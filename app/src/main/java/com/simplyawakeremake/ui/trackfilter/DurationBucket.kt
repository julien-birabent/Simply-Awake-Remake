package com.simplyawakeremake.ui.trackfilter

enum class DurationBucket(
    val minMinutesInclusive: Int,
    val maxMinutesExclusive: Int
) {
    SHORT(minMinutesInclusive = 0, maxMinutesExclusive = 10),
    MEDIUM(minMinutesInclusive = 10, maxMinutesExclusive = 25),
    LONG(minMinutesInclusive = 25, maxMinutesExclusive = Int.MAX_VALUE);

    fun displayLabel(): String {
        return when {
            minMinutesInclusive == 0 -> "< $maxMinutesExclusive"
            maxMinutesExclusive == Int.MAX_VALUE -> "> $minMinutesInclusive"
            else -> "$minMinutesInclusive - $maxMinutesExclusive"
        }
    }
}
