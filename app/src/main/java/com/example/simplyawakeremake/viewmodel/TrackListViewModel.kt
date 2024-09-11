package com.example.simplyawakeremake.viewmodel

import androidx.lifecycle.ViewModel
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.ResultState
import com.example.simplyawakeremake.data.track.TrackRepository
import io.reactivex.rxjava3.core.Flowable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TrackListViewModel : ViewModel(), KoinComponent {

    private val trackRepository: TrackRepository by inject()

    val screenState: Flowable<PlayerListUIState> = trackRepository.getAll().map { resultState ->
        when (resultState) {
            is ResultState.Error -> PlayerListUIState.Error
            is ResultState.Loading -> PlayerListUIState.Loading
            is ResultState.Success -> PlayerListUIState.Tracks(resultState.data.sortedBy { it.ordinal })
        }
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

    data object Error : PlayerListUIState
}
