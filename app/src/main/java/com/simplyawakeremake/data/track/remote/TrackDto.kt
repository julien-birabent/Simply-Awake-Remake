package com.simplyawakeremake.data.track.remote

import android.net.Uri
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.simplyawakeremake.ui.model.Track


data class TrackDto(
    @SerializedName("id")
    val id: String,
    @Expose
    @SerializedName("createDate")
    val createDate: Int,
    @Expose
    @SerializedName("updateDate")
    val updateDate: Int,
    @Expose
    @SerializedName("name")
    val name: String,
    @Expose
    @SerializedName("lengthInSeconds")
    val lengthInSeconds: Int,
    @Expose
    @SerializedName("tagString")
    val tagString: String,
    @Expose
    @SerializedName("season")
    val season: Int,
    @Expose
    @SerializedName("year")
    val year: Int,
    @Expose
    @SerializedName("duration")
    val duration: String
) {
    val audioSourceUri: Uri by lazy { Uri.parse("media/audio/${id}.mp3") }
}

fun TrackDto.toUiTrack(): Track = Track(
    id = id,
    name = name,
    lengthInSeconds = lengthInSeconds,
    tagString = tagString,
    duration = duration
)