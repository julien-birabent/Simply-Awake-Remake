package com.example.simplyawakeremake.data.track

import android.net.Uri

class TrackUriProvider(private val baseUrl: String) {

    fun trackUri(id:String) = baseUrl + Uri.parse("media/audio/${id}.mp3")
}