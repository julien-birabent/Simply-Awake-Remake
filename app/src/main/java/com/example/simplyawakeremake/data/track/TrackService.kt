package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.data.common.ApiService
import io.reactivex.rxjava3.core.Single

interface TrackService : ApiService<ApiTrack> {

    override fun fetchAll() : Single<List<ApiTrack>>
}