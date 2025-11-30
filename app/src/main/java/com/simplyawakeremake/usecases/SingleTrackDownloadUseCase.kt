package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.download.track.TrackDownloadStatus
import com.simplyawakeremake.data.download.track.TrackFileManager

class SingleTrackDownloadUseCase(
    private val trackFileManager: TrackFileManager
) {

    operator fun invoke(trackId: String, trackTitle: String) {
        when (trackFileManager.getDownloadStatus(trackId)) {
            TrackDownloadStatus.NOT_DOWNLOADED -> {
                trackFileManager.downloadSingleTrack(
                    trackId = trackId,
                    trackTitle = trackTitle
                )
            }

            TrackDownloadStatus.DOWNLOADING -> {
                // For now: ignore repeated clicks while downloading.
                // Later you could add per-track cancel behavior here.
            }

            TrackDownloadStatus.DOWNLOADED -> {
                trackFileManager.deleteTrackFile(trackId)
            }
        }
    }
}
