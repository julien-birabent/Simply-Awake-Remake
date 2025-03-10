package com.simplyawakeremake.data.download.track

import android.net.Uri
import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.DownloadSession
import com.simplyawakeremake.data.download.FileStorage
import java.io.File

class TrackFileManager(
    private val downloadService: DownloadService,
    private val fileStorage: FileStorage,
    private val trackUriProvider: (String) -> String
) {

    init {
        fileStorage.ensureDirectoriesExist()
    }

    private var session: DownloadSession? = null
    private val batchSize = 4

    fun cancelAllDownloads() {
        session?.cancel()
        session = null
        downloadService.cancelDownloads()
    }

    fun getTrackFile(trackId: String): File = fileStorage.getTrackFile(trackId)

    fun getTrackUri(trackId: String): Uri {
        val localFile = fileStorage.getTrackFile(trackId)
        return if (localFile.exists()) Uri.fromFile(localFile)
        else Uri.parse(trackUriProvider(trackId))
    }

    fun downloadTracks(
        tracks: List<Pair<String, String>>,
        onDownloadCanceled: () -> Unit,
        onEachTrackDownloaded: (Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        if (session != null) throw IllegalStateException("A download session is already in progress. Call cancelAllDownloads() first.")

        if (areAllTracksDownloaded(tracks)) {
            onComplete(tracks.map { (id, _) -> fileStorage.getTrackFile(id) })
            return
        }

        session = DownloadSession(
            pendingDownloads = tracks.toMutableList(),
            onDownloadCanceled = onDownloadCanceled,
            onEachDownloaded = onEachTrackDownloaded,
            onComplete = onComplete
        )
        session?.startNextBatch(batchSize, ::enqueueSingleDownload)
    }

    private fun enqueueSingleDownload(
        session: DownloadSession,
        track: Pair<String, String>
    ) {
        val (trackId, trackTitle) = track
        val filePath = fileStorage.getTrackFile(trackId)

        downloadService.enqueueDownload(
            url = trackUriProvider(trackId),
            destination = filePath,
            title = trackTitle,
            onCancel = session::cancel
        ) { resultFile ->
            session.handleDownloadResult(resultFile) {
                session.startNextBatch(batchSize, ::enqueueSingleDownload)
            }
        }
    }

    private fun areAllTracksDownloaded(tracks: List<Pair<String, String>>) =
        tracks.all { (id, _) -> fileStorage.getTrackFile(id).exists() }
}