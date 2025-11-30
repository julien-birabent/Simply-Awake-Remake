package com.simplyawakeremake.ui.tracklist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.download.track.TrackDownloadStatus
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.usertrack.sync.InitialUserTrackSyncManager
import com.simplyawakeremake.usecases.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import com.simplyawakeremake.usecases.SingleTrackDownloadUseCase
import com.simplyawakeremake.usecases.ToggleTrackFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

data class TrackUi(
    val id: String,
    val ordinal: Int,
    val displayName: String,
    val tagString: String,
    val duration: String,
    val isFavorite: Boolean,
    val downloadStatus: TrackDownloadStatus,
)

sealed interface TrackListUiState {
    data object Loading : TrackListUiState
    data class Error(val throwable: Throwable) : TrackListUiState
    data class Content(val items: List<TrackUi>) : TrackListUiState
}

class TrackListViewModel(
    private val trackRepository: TrackRepositoryInterface,
    private val downloadTrackListUseCase: DownloadTrackListUseCase,
    private val checkTrackDownloadStatusUseCase: CheckTrackDownloadStatusUseCase,
    private val addTrackToRecentHistoryUseCase: AddTrackToRecentHistoryUseCase,
    private val toggleTrackFavoriteUseCase: ToggleTrackFavoriteUseCase,
    private val initialUserTrackSyncManager: InitialUserTrackSyncManager,
    private val trackFileManager: TrackFileManager,
    private val singleTrackDownloadUseCase: SingleTrackDownloadUseCase,
) : ViewModel(), KoinComponent {

    private val retryTrigger: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

    private val _downloadState = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadState: StateFlow<DownloadProgress> = _downloadState

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tracksFlow: StateFlow<ResultState<List<Track>>> =
        retryTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                fetchTrackList()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                ResultState.Loading(emptyList())
            )

    val uiState: StateFlow<TrackListUiState> =
        combine(
            tracksFlow,
            trackFileManager.downloadingTrackIds, trackFileManager.trackFilesUsage,
        ) { result, downloadingIds, _ ->
            when (result) {
                is ResultState.Success -> {
                    val sortedTracks = result.data.sortedBy { it.ordinal }
                    val uiTracks = mapTracksToUi(
                        tracks = sortedTracks,
                        downloadingIds = downloadingIds,
                        fileManager = trackFileManager
                    )
                    TrackListUiState.Content(uiTracks)
                }

                is ResultState.Error -> TrackListUiState.Error(result.throwable)

                else -> TrackListUiState.Loading
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            TrackListUiState.Loading
        )

    private fun fetchTrackList(): Flow<ResultState<List<Track>>> =
        trackRepository.getAllTracks()
            .onStart { emit(ResultState.Loading(emptyList())) }
            .catch { emit(ResultState.Error(lastData = null, throwable = it)) }

    fun retryLoadingPlaylist() {
        retryTrigger.tryEmit(Unit)
    }

    fun downloadAllTracks() {
        if (_downloadState.value is DownloadProgress.InProgress) return

        val current = tracksFlow.value
        if (current !is ResultState.Success) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tracks = current.data.sortedBy { it.ordinal }
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

    private fun findTrackById(id: String): Track? {
        val current = tracksFlow.value
        return (current as? ResultState.Success)
            ?.data
            ?.firstOrNull { it.id == id }
    }

    fun addToHistory(track: TrackUi) = viewModelScope.launch(Dispatchers.IO) {
        val domainTrack = findTrackById(track.id) ?: return@launch
        addTrackToRecentHistoryUseCase.execute(domainTrack)
    }

    fun onFavoriteClicked(track: TrackUi) {
        viewModelScope.launch(Dispatchers.IO) {
            val domainTrack = findTrackById(track.id) ?: return@launch
            toggleTrackFavoriteUseCase(domainTrack)
        }
    }


    fun onDownloadClicked(track: TrackUi) {
        singleTrackDownloadUseCase(
            trackId = track.id,
            trackTitle = track.displayName
        )
    }

    private fun mapTracksToUi(
        tracks: List<Track>,
        downloadingIds: Set<String>,
        fileManager: TrackFileManager
    ): List<TrackUi> {
        return tracks.map { track ->
            track.toUi(
                isDownloading = downloadingIds.contains(track.id),
                isDownloaded = fileManager.getTrackFile(track.id).exists()
            )
        }
    }

    private fun Track.toUi(
        isDownloading: Boolean,
        isDownloaded: Boolean
    ): TrackUi {
        val status = when {
            isDownloading -> TrackDownloadStatus.DOWNLOADING
            isDownloaded -> TrackDownloadStatus.DOWNLOADED
            else -> TrackDownloadStatus.NOT_DOWNLOADED
        }

        return TrackUi(
            id = this.id,
            ordinal = this.ordinal,
            displayName = this.displayName,
            tagString = this.tagString,
            duration = this.duration,
            isFavorite = this.isFavorite,
            downloadStatus = status
        )
    }

    fun onConnectionAvailableForInitialSync() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                initialUserTrackSyncManager.runIfLoggedInAndNeeded()
            } catch (e: Exception) {
                Log.e("TrackListViewModel", "Initial sync crashed", e)
            }
        }
    }
}
