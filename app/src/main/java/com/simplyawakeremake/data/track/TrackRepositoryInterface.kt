package com.simplyawakeremake.data.track

import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.ui.model.UiTrack
import kotlinx.coroutines.flow.Flow

interface TrackRepositoryInterface {

    fun getTrackBy(id: String): Flow<ResultState<UiTrack>>

    fun getAllTracks() : Flow<ResultState<List<UiTrack>>>
}