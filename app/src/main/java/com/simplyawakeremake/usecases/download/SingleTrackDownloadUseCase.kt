package com.simplyawakeremake.usecases.download

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
                // Later: per-track cancel.
            }

            TrackDownloadStatus.DOWNLOADED -> {
                trackFileManager.deleteTrackFile(trackId)
            }
        }
    }
}
