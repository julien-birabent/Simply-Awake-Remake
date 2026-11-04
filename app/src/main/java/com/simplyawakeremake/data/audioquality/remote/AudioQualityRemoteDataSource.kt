package com.simplyawakeremake.data.audioquality.remote

import com.simplyawakeremake.data.audioquality.AudioQualityLevel

interface AudioQualityRemoteDataSource {
    suspend fun fetchAll(): Map<String, AudioQualityLevel>
}
