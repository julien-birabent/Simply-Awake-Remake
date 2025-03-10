package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.ui.model.UiTrack

class AddTrackToRecentHistoryUseCase(private val historyRepository: TrackHistoryRepositoryInterface) {

    suspend fun execute(uiTrack: UiTrack) = historyRepository.addToHistory(uiTrack)
}