package com.simplyawakeremake.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.usecases.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import com.simplyawakeremake.usecases.ToggleTrackFavoriteUseCase
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
    private val checkTrackDownloadStatusUseCase: CheckTrackDownloadStatusUseCase,
    private val addTrackToRecentHistoryUseCase: AddTrackToRecentHistoryUseCase,
    private val toggleTrackFavoriteUseCase: ToggleTrackFavoriteUseCase,
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

    fun cancelDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadTrackListUseCase.cancelDownloads()
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadProgress.Idle
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return checkTrackDownloadStatusUseCase.execute(trackId)
    }

    fun addToHistory(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        addTrackToRecentHistoryUseCase.execute(track)
    }

    fun onFavoriteClicked(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleTrackFavoriteUseCase(track)
        }
    }

}


sealed interface PlayerListUIState {

    data class Tracks(val items: List<Track>) : PlayerListUIState
    data object Loading : PlayerListUIState
    data class Error(val throwable: Throwable) : PlayerListUIState
}
