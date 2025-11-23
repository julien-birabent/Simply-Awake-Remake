package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.remote.TrackDto
import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

fun TrackDto.toDomain(): TrackEntity = TrackEntity(
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

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    name = name,
    lengthInSeconds = lengthInSeconds,
    tagString = tagString,
    duration = duration,
    season = season,
    year = year
)


fun TrackEntity.toDomain(user: UserTrackEntity?): Track =
    Track(
        id = id,
        name = name,
        lengthInSeconds = lengthInSeconds,
        tagString = tagString,
        duration = duration,
        season = season,
        year = year,
        isFavorite = user?.isFavorite ?: false,
        playCount = user?.playCount ?: 0,
        lastPlayedAt = user?.lastPlayedAt,
    )
