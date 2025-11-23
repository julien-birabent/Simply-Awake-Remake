package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.remote.TrackDto
import com.simplyawakeremake.data.track.Track


fun TrackDto.toEntity(): TrackEntity = TrackEntity(
    id = id,
    createDate = createDate,
    updateDate = updateDate,
    name = name,
    lengthInSeconds = lengthInSeconds,
    tagString = tagString,
    season = season,
    year = year,
    duration = duration,
)

fun TrackEntity.toUiTrack(): Track = Track(
    id = id,
    name = name,
    lengthInSeconds = lengthInSeconds,
    tagString = tagString,
    duration = duration,
)

fun TrackDto.toUiTrack(): Track = Track(
    id = id,
    name = name,
    lengthInSeconds = lengthInSeconds,
    tagString = tagString,
    duration = duration
)
