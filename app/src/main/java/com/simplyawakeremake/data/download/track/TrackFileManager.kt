package com.simplyawakeremake.data.download.track

import android.net.Uri
import androidx.core.net.toUri
import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.DownloadSession
import com.simplyawakeremake.data.download.FileStorage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class TrackFilesUsage(
    val count: Int,
    val totalSizeBytes: Long
)

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

    private val filesChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trackFilesUsage: StateFlow<TrackFilesUsage> = filesChanged
        .onStart { emit(Unit) }
        .mapLatest {
            withContext(Dispatchers.IO) {
                TrackFilesUsage(
                    count = fileStorage.count(),
                    totalSizeBytes = fileStorage.totalSizeBytes()
                )
            }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = TrackFilesUsage(
                count = 0,
                totalSizeBytes = 0L
            )
        )

    private fun markFilesChanged() {
        filesChanged.tryEmit(Unit)
    }

    fun trackCount(): Int = trackFilesUsage.value.count
    fun trackTotalSizeBytes(): Long = trackFilesUsage.value.totalSizeBytes

    fun cancelAllDownloads() {
        session?.cancel()
        session = null
        downloadService.cancelDownloads()
    }

    fun deleteAllTrackFiles(): Boolean {
        cancelAllDownloads()
        val success = fileStorage.deleteAll()
        fileStorage.ensureDirectoriesExist()
        markFilesChanged()
        return success
    }

    fun getTrackFile(trackId: String): File = fileStorage.getTrackFile(trackId)

    fun getTrackUri(trackId: String): Uri {
        val localFile = fileStorage.getTrackFile(trackId)
        return if (localFile.exists()) Uri.fromFile(localFile)
        else trackUriProvider(trackId).toUri()
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
            markFilesChanged()
            return
        }

        session = DownloadSession(
            pendingDownloads = tracks.toMutableList(),
            onDownloadCanceled = onDownloadCanceled,
            onEachDownloaded = { downloadedCount ->
                onEachTrackDownloaded(downloadedCount)
                markFilesChanged()
            },
            onComplete = { files ->
                onComplete(files)
                markFilesChanged()
            }
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
                markFilesChanged()
                session.startNextBatch(batchSize, ::enqueueSingleDownload)
            }
        }
    }

    private fun areAllTracksDownloaded(tracks: List<Pair<String, String>>) =
        tracks.all { (id, _) -> fileStorage.getTrackFile(id).exists() }
}
