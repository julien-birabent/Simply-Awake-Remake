package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.usertrack.UserTrackRepository
import com.simplyawakeremake.now

class RegisterTrackPlayUseCase(
    private val userTrackRepository: UserTrackRepository,
    private val timeProvider: () -> Long = { now() },
) {

    suspend operator fun invoke(trackId: String) {
        val playedAt = timeProvider()

        userTrackRepository.registerPlay(
            trackId = trackId,
            playedAtMillis = playedAt
        )
    }
}
