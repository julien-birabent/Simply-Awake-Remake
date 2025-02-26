package com.simplyawakeremake.domain

import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.UiTrack

class AddTrackToRecentHistoryUseCase(private val historyRepository: TrackHistoryRepositoryInterface) {

    suspend fun execute(uiTrack: UiTrack) = historyRepository.addToHistory(uiTrack)
}