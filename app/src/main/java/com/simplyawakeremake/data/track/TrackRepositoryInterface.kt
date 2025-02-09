package com.simplyawakeremake.data.track

import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.common.ResultState
import io.reactivex.rxjava3.core.Flowable

interface TrackRepositoryInterface {

    fun getTrackBy(id: String): Flowable<ResultState<UiTrack>>
}