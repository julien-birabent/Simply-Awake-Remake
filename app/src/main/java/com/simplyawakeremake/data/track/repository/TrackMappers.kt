package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.remote.TrackDto
import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

fun TrackDto.toDomain(): Track =
    Track(
        id = id,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        duration = duration,
        season = season,
        year = year,
        isFavorite = false,
        playCount = 0,
        lastPlayedAt = null,
    )

fun Track.toDto(): TrackDto =
    TrackDto(
        id = id,
        createDate = 0,
        updateDate = 0,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        season = season,
        year = year,
        duration = duration,
    )

fun TrackEntity.toDomain(): Track =
    Track(
        id = id,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        duration = duration,
        season = season,
        year = year,
        isFavorite = false,
        playCount = 0,
        lastPlayedAt = null,
    )

fun Track.toEntity(): TrackEntity =
    TrackEntity(
        id = id,
        createDate = 0,
        updateDate = 0,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        season = season,
        year = year,
        duration = duration,
    )
