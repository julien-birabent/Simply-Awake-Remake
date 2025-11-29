package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.usertrack.UserTrackRepository

class ToggleTrackFavoriteUseCase(
    private val userTrackRepository: UserTrackRepository,
) {

    suspend fun execute(track: Track) {
        val newFavoriteState = !track.isFavorite
        userTrackRepository.toggleFavorite(
            trackId = track.id,
            isFavorite = newFavoriteState
        )
    }

    suspend operator fun invoke(track: Track) = execute(track)
}
