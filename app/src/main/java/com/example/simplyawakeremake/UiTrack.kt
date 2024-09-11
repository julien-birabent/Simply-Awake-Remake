package com.example.simplyawakeremake

data class UiTrack(
    val id: String,
    val createDate: Int,
    val updateDate: Int,
    val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val season: Int,
    val year: Int,
    val duration: String
) {
    val ordinal : Int = name.substring(0, 2).toInt()
}

