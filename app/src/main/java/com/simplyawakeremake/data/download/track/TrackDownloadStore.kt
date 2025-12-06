package com.simplyawakeremake.data.download.track

import com.simplyawakeremake.data.download.FileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TrackDownloadStore(
    private val fileStorage: FileStorage
) {

    private val _downloadInfos = MutableStateFlow<List<TrackDownloadInfo>>(emptyList())
    val downloadInfos: StateFlow<List<TrackDownloadInfo>> = _downloadInfos.asStateFlow()

    private val _trackFilesUsage = MutableStateFlow(recomputeUsage())
    val trackFilesUsage: StateFlow<TrackFilesUsage> = _trackFilesUsage.asStateFlow()

    fun refreshUsage() {
        _trackFilesUsage.value = recomputeUsage()
    }

    fun setDownloadState(
        trackId: String,
        status: TrackDownloadStatus,
        progress: Float? = null,
    ) {
        _downloadInfos.update { current ->
            val mutable = current.toMutableList()
            val index = mutable.indexOfFirst { it.trackId == trackId }

            if (status == TrackDownloadStatus.NOT_DOWNLOADED && progress == null) {
                if (index >= 0) mutable.removeAt(index)
            } else {
                val info = TrackDownloadInfo(
                    trackId = trackId,
                    status = status,
                    progress = progress
                )
                if (index >= 0) {
                    mutable[index] = info
                } else {
                    mutable.add(info)
                }
            }
            mutable.toList()
        }
    }

    fun clearNonDownloaded() {
        _downloadInfos.update { current ->
            current.filter { it.status == TrackDownloadStatus.DOWNLOADED }
        }
    }

    fun clearAll() {
        _downloadInfos.value = emptyList()
    }

    fun removeTrack(trackId: String) {
        _downloadInfos.update { list ->
            list.filterNot { it.trackId == trackId }
        }
    }

    fun getInfoOrNull(trackId: String): TrackDownloadInfo? =
        _downloadInfos.value.firstOrNull { it.trackId == trackId }

    private fun recomputeUsage(): TrackFilesUsage {
        return TrackFilesUsage(
            count = fileStorage.count(),
            totalSizeBytes = fileStorage.totalSizeBytes()
        )
    }
}
