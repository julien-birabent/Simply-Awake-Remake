package com.simplyawakeremake.data.download.track

import android.net.Uri
import androidx.core.net.toUri
import com.simplyawakeremake.data.download.FileStorage
import java.io.File
import kotlinx.coroutines.flow.StateFlow

class TrackFileManager(
    private val fileStorage: FileStorage,
    private val trackUriProvider: (String) -> String,
    private val downloadStore: TrackDownloadStore,
    private val trackDownloader: TrackDownloader,
) {

    val downloadInfos: StateFlow<List<TrackDownloadInfo>> = downloadStore.downloadInfos
    val trackFilesUsage: StateFlow<TrackFilesUsage> = downloadStore.trackFilesUsage

    init {
        fileStorage.ensureDirectoriesExist()
        downloadStore.refreshUsage()
    }

    fun getDownloadStatus(trackId: String): TrackDownloadStatus {
        val fromState = downloadStore.getInfoOrNull(trackId)?.status
        if (fromState != null) return fromState

        val file = fileStorage.getTrackFile(trackId)
        return if (file.exists()) {
            TrackDownloadStatus.DOWNLOADED
        } else {
            TrackDownloadStatus.NOT_DOWNLOADED
        }
    }

    fun getDownloadInfo(trackId: String): TrackDownloadInfo =
        downloadStore.getInfoOrNull(trackId) ?: TrackDownloadInfo(
            trackId = trackId,
            status = getDownloadStatus(trackId),
            progress = null
        )

    fun getTrackFile(trackId: String): File = fileStorage.getTrackFile(trackId)

    fun getTrackUri(trackId: String): Uri {
        val localFile = fileStorage.getTrackFile(trackId)
        return if (localFile.exists()) Uri.fromFile(localFile)
        else trackUriProvider(trackId).toUri()
    }

    fun cancelAllDownloads() {
        trackDownloader.cancelAllDownloads()
    }

    fun cancelTrackDownload(trackId: String) {
        val destination = fileStorage.getTrackFile(trackId)
        trackDownloader.cancelTrackDownload(trackId, destination)
    }

    fun deleteAllTrackFiles(): Boolean {
        trackDownloader.cancelAllDownloads()
        val success = fileStorage.deleteAll()
        fileStorage.ensureDirectoriesExist()
        downloadStore.clearAll()
        downloadStore.refreshUsage()
        return success
    }

    fun deleteTrackFile(trackId: String): Boolean {
        val file = fileStorage.getTrackFile(trackId)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            downloadStore.removeTrack(trackId)
            downloadStore.refreshUsage()
        }
        return deleted
    }

    fun downloadSingleTrack(
        trackId: String,
        trackTitle: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val request = TrackDownloadRequest(
            trackId = trackId,
            url = trackUriProvider(trackId),
            destination = fileStorage.getTrackFile(trackId),
            title = trackTitle
        )

        trackDownloader.downloadSingleTrack(
            request = request,
            currentStatusResolver = ::getDownloadStatus,
            onResult = onResult
        )
    }

    fun downloadTracks(
        tracks: List<Pair<String, String>>,
        onDownloadCanceled: () -> Unit,
        onEachTrackDownloaded: (Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        val requests = tracks.map { (id, title) ->
            TrackDownloadRequest(
                trackId = id,
                url = trackUriProvider(id),
                destination = fileStorage.getTrackFile(id),
                title = title
            )
        }

        trackDownloader.downloadTracks(
            requests = requests,
            currentStatusResolver = ::getDownloadStatus,
            onDownloadCanceled = onDownloadCanceled,
            onEachTrackDownloaded = onEachTrackDownloaded,
            onComplete = onComplete
        )
    }
}
