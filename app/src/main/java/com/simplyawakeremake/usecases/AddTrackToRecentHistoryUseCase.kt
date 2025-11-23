package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.ui.model.Track

class AddTrackToRecentHistoryUseCase(private val historyRepository: TrackHistoryRepositoryInterface) {

    suspend fun execute(track: Track) = historyRepository.addToHistory(track)
}