package com.simplyawakeremake.data.track

import retrofit2.http.GET

interface TrackService {

    @GET("api/tracks")
    suspend fun fetchAll(): List<ApiTrack>
}