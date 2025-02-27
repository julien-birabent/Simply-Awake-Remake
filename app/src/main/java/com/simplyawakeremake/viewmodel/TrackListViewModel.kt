package com.simplyawakeremake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

class TrackListViewModel(
    val app: Application,
    private val trackRepository: TrackRepositoryInterface
) : AndroidViewModel(app), KoinComponent {

    private val retryTrigger: MutableStateFlow<Unit> = MutableStateFlow(Unit)

    @OptIn(ExperimentalCoroutinesApi::class)
    val screenState: StateFlow<PlayerListUIState> =
        retryTrigger.flatMapLatest {
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
        retryTrigger.value = Unit
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
