package com.example.simplyawakeremake

import android.net.Uri

data class UiTrack(
    val id: String,
    val name: String,
    val lengthInSeconds: Int,
    val tagString: String,
    val duration: String
) {
    val ordinal : Int = name.substring(0, 2).toInt()
    val audioSourceUri : Uri by lazy { Uri.parse("media/audio/${id}.mp3") }
}

