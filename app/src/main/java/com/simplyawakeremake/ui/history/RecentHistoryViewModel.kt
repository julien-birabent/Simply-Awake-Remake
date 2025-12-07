package com.simplyawakeremake.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.history.GetRecentHistoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

private const val MAX_HISTORY_LIMIT = 20

class RecentHistoryViewModel(
    private val getRecentHistoryUseCase: GetRecentHistoryUseCase,
    private val checkTrackDownloadStatusUseCase: CheckTrackDownloadStatusUseCase
) : ViewModel(), KoinComponent {

    private var historyLimit: Int = MAX_HISTORY_LIMIT

    val uiState: StateFlow<RecentHistoryUIState> =
        loadHistory().stateIn(viewModelScope, SharingStarted.Lazily, RecentHistoryUIState.Loading)

    private fun loadHistory(): Flow<RecentHistoryUIState> =
        getRecentHistoryUseCase.execute(historyLimit)
            .map { result ->
                when (result) {
                    is ResultState.Success -> RecentHistoryUIState.RecentHistoryLoaded(result.data.sortedByDescending { it.playedTimestamp })
                    is ResultState.Error -> RecentHistoryUIState.Error(result.throwable)
                    is ResultState.Loading -> RecentHistoryUIState.Loading
                }
            }

    fun isTrackDownloaded(trackId: String): Boolean {
        return checkTrackDownloadStatusUseCase.execute(trackId)
    }

    sealed interface RecentHistoryUIState {
        data object Loading : RecentHistoryUIState
        data class Error(val throwable: Throwable) : RecentHistoryUIState
        data class RecentHistoryLoaded(val items: List<UiTrackHistory>) : RecentHistoryUIState
    }
}