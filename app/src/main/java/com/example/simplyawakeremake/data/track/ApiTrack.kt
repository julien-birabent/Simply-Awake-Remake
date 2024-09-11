package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.UiTrack
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


data class ApiTrack(
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
)

//TODO To be updated at a later stage when I know what the UI needs data wise and in which format.
fun ApiTrack.toUiTrack(): UiTrack = UiTrack(
    id,
    createDate,
    updateDate,
    name,
    lengthInSeconds,
    tagString,
    season,
    year,
    duration
)