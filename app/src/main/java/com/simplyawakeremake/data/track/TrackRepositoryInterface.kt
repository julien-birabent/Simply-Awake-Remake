package com.simplyawakeremake.data.track

import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.common.ResultState
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.flow.Flow

interface TrackRepositoryInterface {

    fun getTrackBy(id: String): Flow<ResultState<UiTrack>>

    fun getAllTracks() : Flow<ResultState<List<UiTrack>>>
}