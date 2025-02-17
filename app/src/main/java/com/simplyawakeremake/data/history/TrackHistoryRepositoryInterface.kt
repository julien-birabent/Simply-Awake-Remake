package com.simplyawakeremake.data.history

import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.UiTrack
import kotlinx.coroutines.flow.Flow

internal interface TrackHistoryRepositoryInterface {

    val maxAmountStored : Int

    fun getRecentlyPlayedHistory(historyLimit : Int) : Flow<ResultState<List<UiTrackHistory>>>

    suspend fun addToHistory(uiTrack: UiTrack)
}