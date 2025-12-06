package com.simplyawakeremake.usecases.download

import com.simplyawakeremake.data.download.track.TrackDownloadStatus
import com.simplyawakeremake.data.download.track.TrackFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class ObserveActiveDownloadsUseCase(
    private val trackFileManager: TrackFileManager
) {

    operator fun invoke(): Flow<ActiveDownloadsState> {
        return trackFileManager.downloadInfos
            .map { infos ->
                val remaining = infos.count { info ->
                    info.status == TrackDownloadStatus.DOWNLOADING
                }

                ActiveDownloadsState(
                    hasActiveDownloads = remaining > 0,
                    remainingTracks = remaining
                )
            }
    }
}

data class ActiveDownloadsState(
    val hasActiveDownloads: Boolean,
    val remainingTracks: Int,
)
