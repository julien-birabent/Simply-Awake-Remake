package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.remote.TrackDto

fun TrackDto.toDomain(): Track =
    Track(
        id = id,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        createdAt = createDate,
        duration = duration,
        season = season,
        year = year,
        isFavorite = false,
        playCount = 0,
        lastPlayedAt = null,
    )

fun TrackEntity.toDomain(): Track =
    Track(
        id = id,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        duration = duration,
        season = season,
        createdAt = createDate,
        year = year,
        isFavorite = false,
        playCount = 0,
        lastPlayedAt = null,
    )

fun Track.toEntity(): TrackEntity =
    TrackEntity(
        id = id,
        createDate = createdAt,
        updateDate = 0,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        season = season,
        year = year,
        duration = duration,
    )
