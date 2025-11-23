package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.ui.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File

class DownloadTrackListUseCase(private val trackFileManager: TrackFileManager) {

    private var currentJob: Job? = null
    private var currentScope: CoroutineScope? = null

    fun execute(tracks: List<Track>): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress.InProgress(0))

        currentScope = CoroutineScope(Dispatchers.IO)
        currentJob = currentScope?.launch {
            try {
                trackFileManager.downloadTracks(
                    tracks.map { it.id to it.displayName },
                    onDownloadCanceled = {
                        trySend(DownloadProgress.Failure(CancellationException("Download manually canceled.")))
                        close()
                    },
                    onEachTrackDownloaded = { progressPercentage ->
                        trySend(DownloadProgress.InProgress(progressPercentage))
                    }) { downloadedFiles ->
                    trySend(DownloadProgress.Success(downloadedFiles))
                    close()
                }
            } catch (e: Exception) {
                trySend(DownloadProgress.Failure(e))
                close()
            }
        }

        awaitClose { }
    }.catch {
        emit(DownloadProgress.Failure(it))
    }.flowOn(Dispatchers.IO)

    fun cancelDownloads() {
        currentJob?.cancel()
        trackFileManager.cancelAllDownloads()
    }
}


sealed class DownloadProgress {
    data object Idle : DownloadProgress()
    data class InProgress(val percentage: Int) : DownloadProgress()
    data class Success(val files: List<File>) : DownloadProgress()
    data class Failure(val error: Throwable) : DownloadProgress()
}
