package com.simplyawakeremake.data.usertrack.sync

sealed class UserTrackSyncResult {
    data class Success(
        val uploadedCount: Int = 0,
        val downloadedCount: Int = 0,
    ) : UserTrackSyncResult()

    data class Failure(val cause: Throwable) : UserTrackSyncResult()
}