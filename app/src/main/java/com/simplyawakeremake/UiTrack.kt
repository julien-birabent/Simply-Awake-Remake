package com.simplyawakeremake

data class UiTrack(
    val id: String,
    private val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val duration: String
) {
    val ordinal: Int = name.substring(0, 3).removeRange(0, 1).toInt()
    val displayName: String = name.replace(Regex("\\d"),"").trim()
}

