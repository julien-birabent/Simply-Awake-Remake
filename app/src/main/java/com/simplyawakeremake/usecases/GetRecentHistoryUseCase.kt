package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.history.UiTrackHistory
import kotlinx.coroutines.flow.Flow

class GetRecentHistoryUseCase(private val historyRepository: TrackHistoryRepositoryInterface) {

    fun execute(historyLimit: Int): Flow<ResultState<List<UiTrackHistory>>> =
        historyRepository.getRecentlyPlayedHistory(historyLimit)
}