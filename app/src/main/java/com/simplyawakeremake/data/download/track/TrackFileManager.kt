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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class TrackFilesUsage(
    val count: Int,
    val totalSizeBytes: Long
)

enum class TrackDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
}

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

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private fun updateDownloading(
        add: Set<String> = emptySet(),
        remove: Set<String> = emptySet()
    ) {
        _downloadingTrackIds.value =
            _downloadingTrackIds.value
                .plus(add)
                .minus(remove)
    }

    private fun markFilesChanged() {
        filesChanged.tryEmit(Unit)
    }

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

    fun trackCount(): Int = trackFilesUsage.value.count

    fun cancelAllDownloads() {
        session?.cancel()
        session = null
        downloadService.cancelDownloads()
        _downloadingTrackIds.value = emptySet()
    }

    fun deleteAllTrackFiles(): Boolean {
        cancelAllDownloads()
        val success = fileStorage.deleteAll()
        fileStorage.ensureDirectoriesExist()
        markFilesChanged()
        return success
    }

    fun deleteTrackFile(trackId: String): Boolean {
        val file = fileStorage.getTrackFile(trackId)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            markFilesChanged()
        }
        return deleted
    }

    fun getDownloadStatus(trackId: String): TrackDownloadStatus {
        val isDownloading = _downloadingTrackIds.value.contains(trackId)
        val isDownloaded = fileStorage.getTrackFile(trackId).exists()

        return when {
            isDownloading -> TrackDownloadStatus.DOWNLOADING
            isDownloaded -> TrackDownloadStatus.DOWNLOADED
            else -> TrackDownloadStatus.NOT_DOWNLOADED
        }
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
        if (session != null) {
            throw IllegalStateException("A download session is already in progress. Call cancelAllDownloads() first.")
        }

        if (areAllTracksDownloaded(tracks)) {
            onComplete(tracks.map { (id, _) -> fileStorage.getTrackFile(id) })
            markFilesChanged()
            return
        }

        val trackIds = tracks.map { it.first }.toSet()
        updateDownloading(add = trackIds)

        session = DownloadSession(
            pendingDownloads = tracks.toMutableList(),
            onDownloadCanceled = {
                updateDownloading(remove = trackIds)
                onDownloadCanceled()
            },
            onEachDownloaded = { downloadedCount ->
                onEachTrackDownloaded(downloadedCount)
                markFilesChanged()
            },
            onComplete = { files ->
                updateDownloading(remove = trackIds)
                markFilesChanged()
                onComplete(files)
            }
        )
        session?.startNextBatch(batchSize, ::enqueueSingleDownload)
    }

    fun downloadSingleTrack(
        trackId: String,
        trackTitle: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (_downloadingTrackIds.value.contains(trackId)) return

        updateDownloading(add = setOf(trackId))

        val destination = fileStorage.getTrackFile(trackId)

        downloadService.enqueueDownload(
            url = trackUriProvider(trackId),
            destination = destination,
            title = trackTitle,
            onCancel = {
                updateDownloading(remove = setOf(trackId))
                markFilesChanged()
                onResult(false)
            }
        ) { resultFile ->
            val success = resultFile != null && resultFile.exists()
            updateDownloading(remove = setOf(trackId))
            markFilesChanged()
            onResult(success)
        }
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
