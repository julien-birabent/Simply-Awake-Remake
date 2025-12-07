package com.simplyawakeremake.data.download.track

import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.DownloadSession
import java.io.File

data class TrackDownloadRequest(
    val trackId: String,
    val url: String,
    val destination: File,
    val title: String,
)

class TrackDownloader(
    private val downloadService: DownloadService,
    private val downloadStore: TrackDownloadStore,
) {

    private var session: DownloadSession? = null
    private val batchSize = 4
    private var requestById: Map<String, TrackDownloadRequest> = emptyMap()

    fun cancelAllDownloads() {
        session?.cancel()
        session = null
        downloadService.cancelDownloads()
        downloadStore.clearNonDownloaded()
    }

    fun cancelTrackDownload(trackId: String, destination: File) {
        downloadService.cancelDownload(destination)
        downloadStore.setDownloadState(
            trackId = trackId,
            status = TrackDownloadStatus.NOT_DOWNLOADED
        )
    }

    fun downloadSingleTrack(
        request: TrackDownloadRequest,
        currentStatusResolver: (String) -> TrackDownloadStatus,
        onResult: (Boolean) -> Unit = {}
    ) {
        val trackId = request.trackId

        if (currentStatusResolver(trackId) == TrackDownloadStatus.DOWNLOADING) {
            return
        }

        downloadStore.setDownloadState(trackId, TrackDownloadStatus.DOWNLOADING)

        downloadService.enqueueDownload(
            url = request.url,
            destination = request.destination,
            title = request.title,
            onCancel = {
                downloadStore.setDownloadState(trackId, TrackDownloadStatus.NOT_DOWNLOADED)
                onResult(false)
            }
        ) { resultFile ->
            val success = resultFile != null && resultFile.exists()
            if (success) {
                downloadStore.setDownloadState(trackId, TrackDownloadStatus.DOWNLOADED)
                downloadStore.refreshUsage()
            } else {
                downloadStore.setDownloadState(trackId, TrackDownloadStatus.NOT_DOWNLOADED)
            }
            onResult(success)
        }
    }

    fun downloadTracks(
        requests: List<TrackDownloadRequest>,
        currentStatusResolver: (String) -> TrackDownloadStatus,
        onDownloadCanceled: () -> Unit,
        onEachTrackDownloaded: (Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        if (session != null) {
            throw IllegalStateException("A download session is already in progress. Call cancelAllDownloads() first.")
        }

        requestById = requests.associateBy { it.trackId }

        val (alreadyDownloaded, toDownload) = requests.partition { it.destination.exists() }

        if (toDownload.isEmpty()) {
            onComplete(alreadyDownloaded.map { it.destination })
            return
        }

        val toDownloadIds = toDownload.map { it.trackId }.toSet()
        toDownloadIds.forEach { id ->
            downloadStore.setDownloadState(id, TrackDownloadStatus.DOWNLOADING)
        }

        session = DownloadSession(
            pendingDownloads = toDownload
                .map { it.trackId to it.title }
                .toMutableList(),
            onDownloadCanceled = {
                toDownloadIds.forEach { id ->
                    if (currentStatusResolver(id) != TrackDownloadStatus.DOWNLOADED) {
                        downloadStore.setDownloadState(id, TrackDownloadStatus.NOT_DOWNLOADED)
                    }
                }
                onDownloadCanceled()
            },
            onEachDownloaded = { downloadedCount ->
                onEachTrackDownloaded(downloadedCount)
            },
            onComplete = { downloadedFiles ->
                val allFiles = alreadyDownloaded.map { it.destination } + downloadedFiles
                downloadStore.refreshUsage()
                onComplete(allFiles)
            }
        )

        session?.startNextBatch(batchSize, ::enqueueSingleDownload)
    }

    private fun enqueueSingleDownload(
        session: DownloadSession,
        track: Pair<String, String>
    ) {
        val (trackId, _) = track
        val request = requestById[trackId]
            ?: error("Missing TrackDownloadRequest for id=$trackId")

        val (id, url, destination, title) = request

        downloadService.enqueueDownload(
            url = url,
            destination = destination,
            title = title,
            onCancel = session::cancel
        ) { resultFile ->
            val success = resultFile != null && resultFile.exists()
            if (success) {
                downloadStore.setDownloadState(id, TrackDownloadStatus.DOWNLOADED)
            } else {
                downloadStore.setDownloadState(id, TrackDownloadStatus.NOT_DOWNLOADED)
            }
            downloadStore.refreshUsage()

            session.handleDownloadResult(resultFile) {
                session.startNextBatch(batchSize, ::enqueueSingleDownload)
            }
        }
    }

}
