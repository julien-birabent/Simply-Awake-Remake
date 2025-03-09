package com.simplyawakeremake.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import com.simplyawakeremake.ui.model.UiTrack
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class TrackListViewModel(
    private val trackRepository: TrackRepositoryInterface,
    private val downloadTrackListUseCase: DownloadTrackListUseCase,
    private val checkTrackDownloadStatusUseCase: CheckTrackDownloadStatusUseCase
) : ViewModel(), KoinComponent {

    private val retryTrigger: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)
    private val _downloadState = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadState: StateFlow<DownloadProgress> = _downloadState

    @OptIn(ExperimentalCoroutinesApi::class)
    val screenState: StateFlow<PlayerListUIState> =
        retryTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                fetchTrackList()
            }.stateIn(viewModelScope, SharingStarted.Lazily, PlayerListUIState.Loading)

    private fun fetchTrackList(): Flow<PlayerListUIState> =
        trackRepository.getAllTracks()
            .map { result ->
                when (result) {
                    is ResultState.Success -> PlayerListUIState.Tracks(result.data.sortedBy { it.ordinal })
                    is ResultState.Error -> PlayerListUIState.Error(result.throwable)
                    else -> PlayerListUIState.Loading
                }
            }
            .onStart { emit(PlayerListUIState.Loading) }
            .catch { emit(PlayerListUIState.Error(it)) }

    fun retryLoadingPlaylist() {
        retryTrigger.tryEmit(Unit)
    }

    fun downloadAllTracks() {
        if (_downloadState.value is DownloadProgress.InProgress || screenState.value !is PlayerListUIState.Tracks) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tracks = (screenState.value as PlayerListUIState.Tracks).items
                downloadTrackListUseCase.execute(tracks)
                    .collect { progress ->
                        _downloadState.value = progress
                    }
            } catch (e: Exception) {
                _downloadState.value = DownloadProgress.Failure(e)
            }
        }
    }

    fun cancelDownload(){
        viewModelScope.launch(Dispatchers.IO){
            downloadTrackListUseCase.cancelDownloads()
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadProgress.Idle
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return checkTrackDownloadStatusUseCase.execute(trackId)
    }
}

/**
 * Sealed interface representing the different states of the player UI.
 */
sealed interface PlayerListUIState {
    /**
     * Represents the state when the player UI displays a list of tracks.
     *
     * @property items The list of track items to be displayed.
     */
    data class Tracks(val items: List<UiTrack>) : PlayerListUIState

    /**
     * Represents the state when the player UI is in a loading state.
     */
    data object Loading : PlayerListUIState

    data class Error(val throwable: Throwable) : PlayerListUIState
}
