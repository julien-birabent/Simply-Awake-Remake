package com.simplyawakeremake.data.track.remote

import retrofit2.http.GET

interface TrackService {

    @GET("api/tracks")
    suspend fun fetchAll(): List<TrackDto>
}