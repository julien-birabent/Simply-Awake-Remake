package com.simplyawakeremake.ui.tracklist

import com.simplyawakeremake.data.audioquality.AudioQualityLevel
import com.simplyawakeremake.data.download.track.TrackDownloadStatus

data class TrackUi(
    val id: String,
    val ordinal: Int,
    val displayName: String,
    val tagString: String,
    val duration: String,
    val isFavorite: Boolean,
    val downloadStatus: TrackDownloadStatus,
    val playCount: Int,
    val audioQuality: AudioQualityLevel? = null,
)