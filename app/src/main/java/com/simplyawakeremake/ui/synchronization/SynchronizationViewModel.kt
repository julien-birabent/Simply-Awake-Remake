package com.simplyawakeremake.ui.synchronization

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.usecases.SyncUserTracksAfterLoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SynchronizationViewModel(
    private val syncUserTracksAfterLoginUseCase: SyncUserTracksAfterLoginUseCase,
) : ViewModel() {

    private val TAG = "SynchronizationViewModel"

    private val _uiState = MutableStateFlow(
        SyncUiState(
            isVisible = false,
            isRunning = false,
            message = ""
        )
    )
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>()
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    fun startSync() {

        if (_uiState.value.isRunning) return

        viewModelScope.launch {
            _uiState.value = SyncUiState(
                isVisible = true,
                isRunning = true,
                message = "Synchronising your data…"
            )

            val result: UserTrackSyncResult = try {
                syncUserTracksAfterLoginUseCase()
            } catch (e: Exception) {
                Log.e(TAG, "startSyncAfterLogin: unexpected error", e)
                UserTrackSyncResult.Failure(e)
            }

            val baseState = _uiState.value.copy(
                isRunning = false,
                errorMessage = null
            )

            _uiState.value = when (result) {
                is UserTrackSyncResult.Success -> baseState.copy(
                    message = "Synchronization completed.",
                    uploadedCount = result.uploadedCount,
                    downloadedCount = result.downloadedCount,
                    errorMessage = null
                )
                is UserTrackSyncResult.Failure -> baseState.copy(
                    message = "We couldn't synchronise your data.",
                    errorMessage = result.cause.message ?: "Unknown error"
                )
            }

            _events.emit(SyncEvent.Completed(result))
        }
    }
}
