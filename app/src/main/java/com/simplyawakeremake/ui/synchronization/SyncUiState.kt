package com.simplyawakeremake.ui.synchronization

import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult

data class SyncUiState(
    val isVisible: Boolean = false,
    val isRunning: Boolean = false,
    val message: String = "",
    val errorMessage: String? = null,
    val uploadedCount: Int? = null,
    val downloadedCount: Int? = null,
)

sealed interface SyncEvent {
    data class Completed(val result: UserTrackSyncResult) : SyncEvent
}