package com.simplyawakeremake.usecases

import com.simplyawakeremake.data.download.track.TrackFileManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteAllDownloadsUseCase(
    private val trackFileManager: TrackFileManager
) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        trackFileManager.deleteAllTrackFiles()
    }
}