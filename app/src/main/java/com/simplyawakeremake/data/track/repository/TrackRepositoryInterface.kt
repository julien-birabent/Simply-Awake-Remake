package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepositoryInterface {

    fun getTrackBy(id: String): Flow<ResultState<Track>>

    fun getAllTracks(): Flow<ResultState<List<Track>>>
}