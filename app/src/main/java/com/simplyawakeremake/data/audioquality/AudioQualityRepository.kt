package com.simplyawakeremake.data.audioquality

import android.util.Log
import com.simplyawakeremake.data.audioquality.remote.AudioQualityRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class AudioQualityRepository(
    private val remote: AudioQualityRemoteDataSource,
) {

    private val TAG = "AudioQualityRepository"

    fun observeAll(): Flow<Map<String, AudioQualityLevel>> = flow {
        emit(emptyMap())
        emit(remote.fetchAll())
    }.catch { e ->
        Log.e(TAG, "Audio quality fetch failed, tracks stay unrated", e)
    }
}
