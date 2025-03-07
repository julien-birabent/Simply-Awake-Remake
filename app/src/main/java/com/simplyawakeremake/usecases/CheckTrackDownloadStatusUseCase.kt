package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.track.TrackFileManager

class CheckTrackDownloadStatusUseCase(private val trackFileManager: TrackFileManager) {
    fun execute(trackId: String): Boolean {
        return trackFileManager.getTrackFile(trackId).exists()
    }
}
