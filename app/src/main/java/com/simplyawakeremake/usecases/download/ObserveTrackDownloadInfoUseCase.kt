package com.simplyawakeremake.usecases.download

import com.simplyawakeremake.data.download.track.TrackDownloadInfo
import com.simplyawakeremake.data.download.track.TrackFileManager
import kotlinx.coroutines.flow.StateFlow

class ObserveTrackDownloadsUseCase(
    private val trackFileManager: TrackFileManager
) {

    operator fun invoke(): StateFlow<List<TrackDownloadInfo>> {
        return trackFileManager.downloadInfos
    }
}
