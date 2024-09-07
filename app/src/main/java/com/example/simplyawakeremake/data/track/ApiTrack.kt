package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.UiTrack

data class ApiTrack(
    val createDate: Int,
    val duration: String,
    val id: String,
    val lengthInSeconds: Int,
    val name: String,
    val season: Int,
    val tagString: String,
    val updateDate: Int,
    val year: Int
)
//TODO To be updated at a later stage when I know what the UI needs data wise and in which format.
fun ApiTrack.toUiTrack() : UiTrack = UiTrack(
    createDate,
    duration,
    id,
    lengthInSeconds,
    name,
    season,
    tagString,
    updateDate,
    year
)