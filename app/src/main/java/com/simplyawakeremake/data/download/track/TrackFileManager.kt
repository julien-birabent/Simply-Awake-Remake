package com.simplyawakeremake.data.download.track

import android.net.Uri
import androidx.core.net.toUri
import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.DownloadSession
import com.simplyawakeremake.data.download.FileStorage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackFileManager(
    private val downloadService: DownloadService,
    private val fileStorage: FileStorage,
    private val trackUriProvider: (String) -> String,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var session: DownloadSession? = null
    private val batchSize = 4

    private val _downloadInfos = MutableStateFlow<List<TrackDownloadInfo>>(emptyList())
    val downloadInfos: StateFlow<List<TrackDownloadInfo>> = _downloadInfos.asStateFlow()

    val trackFilesUsage: StateFlow<TrackFilesUsage> =
        downloadInfos
            .map { recomputeUsage() }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                TrackFilesUsage(0, 0L)
            )

    init {
        fileStorage.ensureDirectoriesExist()
    }

    fun getDownloadStatus(trackId: String): TrackDownloadStatus {
        val fromState = _downloadInfos.value.firstOrNull { it.trackId == trackId }?.status
        if (fromState != null) return fromState

        val file = fileStorage.getTrackFile(trackId)
        return if (file.exists()) TrackDownloadStatus.DOWNLOADED
        else TrackDownloadStatus.NOT_DOWNLOADED
    }

    fun getDownloadInfo(trackId: String): TrackDownloadInfo =
        _downloadInfos.value.firstOrNull { it.trackId == trackId }
            ?: TrackDownloadInfo(
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
        session?.cancel()
        session = null
        downloadService.cancelDownloads()

        _downloadInfos.update { current ->
            current.filter { it.status == TrackDownloadStatus.DOWNLOADED }
        }
    }

    fun deleteAllTrackFiles(): Boolean {
        cancelAllDownloads()
        val success = fileStorage.deleteAll()
        fileStorage.ensureDirectoriesExist()
        _downloadInfos.value = emptyList()
        return success
    }

    fun deleteTrackFile(trackId: String): Boolean {
        val file = fileStorage.getTrackFile(trackId)
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            _downloadInfos.update { list ->
                list.filterNot { it.trackId == trackId }
            }
        }
        return deleted
    }

    fun downloadSingleTrack(
        trackId: String,
        trackTitle: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (getDownloadStatus(trackId) == TrackDownloadStatus.DOWNLOADING) return

        setDownloadState(trackId, TrackDownloadStatus.DOWNLOADING)

        val destination = fileStorage.getTrackFile(trackId)

        downloadService.enqueueDownload(
            url = trackUriProvider(trackId),
            destination = destination,
            title = trackTitle,
            onCancel = {
                setDownloadState(trackId, TrackDownloadStatus.NOT_DOWNLOADED)
                onResult(false)
            }
        ) { resultFile ->
            val success = resultFile != null && resultFile.exists()
            if (success) {
                setDownloadState(trackId, TrackDownloadStatus.DOWNLOADED)
            } else {
                setDownloadState(trackId, TrackDownloadStatus.NOT_DOWNLOADED)
            }
            onResult(success)
        }
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

        val (alreadyDownloaded, toDownload) = tracks.partition { (id, _) ->
            fileStorage.getTrackFile(id).exists()
        }

        if (toDownload.isEmpty()) {
            onComplete(alreadyDownloaded.map { (id, _) -> fileStorage.getTrackFile(id) })
            return
        }

        val toDownloadIds = toDownload.map { it.first }.toSet()
        toDownloadIds.forEach { id ->
            setDownloadState(id, TrackDownloadStatus.DOWNLOADING)
        }

        session = DownloadSession(
            pendingDownloads = toDownload.toMutableList(),
            onDownloadCanceled = {
                toDownloadIds.forEach { id ->
                    if (getDownloadStatus(id) != TrackDownloadStatus.DOWNLOADED) {
                        setDownloadState(id, TrackDownloadStatus.NOT_DOWNLOADED)
                    }
                }
                onDownloadCanceled()
            },
            onEachDownloaded = { downloadedCount ->
                onEachTrackDownloaded(downloadedCount)
            },
            onComplete = { downloadedFiles ->
                val allFiles = alreadyDownloaded.map { (id, _) ->
                    fileStorage.getTrackFile(id) } + downloadedFiles
                onComplete(allFiles)
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
                val success = resultFile != null && resultFile.exists()
                if (success) {
                    setDownloadState(trackId, TrackDownloadStatus.DOWNLOADED)
                } else {
                    setDownloadState(trackId, TrackDownloadStatus.NOT_DOWNLOADED)
                }
                session.startNextBatch(batchSize, ::enqueueSingleDownload)
            }
        }
    }

    private fun setDownloadState(
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

    private fun recomputeUsage(): TrackFilesUsage {
        return TrackFilesUsage(
            count = fileStorage.count(),
            totalSizeBytes = fileStorage.totalSizeBytes()
        )
    }
}
