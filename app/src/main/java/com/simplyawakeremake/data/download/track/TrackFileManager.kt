package com.simplyawakeremake.data.download.track

import android.net.Uri
import android.util.Log
import com.simplyawakeremake.data.download.DownloadService
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

    private var pendingDownloads: MutableList<Pair<String, String>> = mutableListOf()
    private val downloadedFiles = mutableListOf<File>()
    private val batchSize = 4

    fun cancelAllDownloads() {
        pendingDownloads.clear()
        downloadService.cancelDownloads()
    }

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
        if (areAllTracksDownloaded(tracks)) {
            onComplete(tracks.map { (id, _) -> fileStorage.getTrackFile(id) })
            return
        }

        pendingDownloads = tracks.toMutableList()
        downloadedFiles.clear()
        startNextBatch(onDownloadCanceled, onEachTrackDownloaded, onComplete)
    }

    private fun startNextBatch(
        onDownloadCanceled: () -> Unit,
        onEachTrackDownloaded: (Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        if (pendingDownloads.isEmpty()) {
            onComplete(downloadedFiles)
            return
        }

        val batch = pendingDownloads.take(batchSize)
        var batchCount = batch.size
        pendingDownloads = pendingDownloads.drop(batchSize).toMutableList()

        batch.forEach { (trackId, trackTitle) ->
            val filePath = fileStorage.getTrackFile(trackId)
            downloadService.enqueueDownload(
                url = trackUriProvider(trackId),
                destination = filePath,
                title = trackTitle,
                onCancel = {
                    pendingDownloads.clear()
                    onDownloadCanceled()
                }
            ) { resultFile ->
                batchCount -= 1
                resultFile?.let {
                    downloadedFiles.add(it)
                    onEachTrackDownloaded((downloadedFiles.size * 100) / (downloadedFiles.size + pendingDownloads.size))
                }
                if (pendingDownloads.isEmpty()) {
                    onComplete(downloadedFiles)
                } else if(batchCount == 0){
                    startNextBatch(onDownloadCanceled, onEachTrackDownloaded, onComplete)
                }
            }
        }
    }

    private fun areAllTracksDownloaded(tracks: List<Pair<String, String>>) =
        tracks.all { (id, _) -> fileStorage.getTrackFile(id).exists() }

    fun getTrackFile(trackId: String): File = fileStorage.getTrackFile(trackId)
}

