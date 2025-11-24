package com.simplyawakeremake.data.history

import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.Track
import kotlinx.coroutines.flow.Flow

interface TrackHistoryRepositoryInterface {

    val maxAmountStored: Int

    fun getRecentlyPlayedHistory(historyLimit: Int): Flow<ResultState<List<UiTrackHistory>>>

    suspend fun addToHistory(track: Track)
}