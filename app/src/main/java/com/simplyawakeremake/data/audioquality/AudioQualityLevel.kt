package com.simplyawakeremake.data.audioquality

enum class AudioQualityLevel {
    GOOD,
    FAIR,
    POOR;

    companion object {
        fun fromName(name: String?): AudioQualityLevel? = entries.firstOrNull { it.name == name }
    }
}
