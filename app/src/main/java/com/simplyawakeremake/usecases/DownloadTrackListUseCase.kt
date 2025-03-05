package com.simplyawakeremake.usecases

import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.track.TrackFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.io.File

class DownloadTrackListUseCase(private val trackFileManager: TrackFileManager) {

    fun execute(tracks: List<UiTrack>): Flow<DownloadProgress> = callbackFlow {
        trySend(DownloadProgress.InProgress(0))
        try {
            trackFileManager.downloadTracks(tracks.map { it.id to it.displayName }, { progressPercentage ->
                trySend(DownloadProgress.InProgress(progressPercentage))
            }) { downloadedFiles ->
                trySend(DownloadProgress.Success(downloadedFiles))
                close()
            }
            awaitClose {
                cancel()
            }
        } catch (e: Exception) {
            trySend(DownloadProgress.Failure(e))
        }
    }.catch { emit(DownloadProgress.Failure(it)) }
        .flowOn(Dispatchers.IO)

}

sealed class DownloadProgress {
    data object Idle : DownloadProgress()
    data class InProgress(val percentage: Int) : DownloadProgress()
    data class Success(val files: List<File>) : DownloadProgress()
    data class Failure(val error: Throwable) : DownloadProgress()
}
