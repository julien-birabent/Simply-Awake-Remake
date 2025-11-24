package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.Track

class AddTrackToRecentHistoryUseCase(private val historyRepository: TrackHistoryRepositoryInterface) {

    suspend fun execute(track: Track) = historyRepository.addToHistory(track)
}