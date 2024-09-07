package com.example.simplyawakeremake.data.track

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface TrackService {

    @GET("api/tracks")
    fun fetchAll(): Single<List<ApiTrack>>
}