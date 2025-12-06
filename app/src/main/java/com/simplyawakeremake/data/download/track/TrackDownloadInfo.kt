package com.simplyawakeremake.data.download.track

data class TrackDownloadInfo(
    val trackId: String,
    val status: TrackDownloadStatus,
    val progress: Float? = null,
    // later: val lastError: Throwable? = null, etc.
)

enum class TrackDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
}
