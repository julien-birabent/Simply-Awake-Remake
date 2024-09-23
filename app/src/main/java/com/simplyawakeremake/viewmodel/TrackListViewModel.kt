package com.simplyawakeremake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepository
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TrackListViewModel(val app: Application) : AndroidViewModel(app), KoinComponent {

    private val trackRepository: TrackRepository by inject()
    private val retryProcessor: BehaviorProcessor<Unit> = BehaviorProcessor.createDefault(Unit)
    private val playListRequest: Flowable<ResultState<List<UiTrack>>>
        get() = trackRepository.getAll()

    val screenState: Flowable<PlayerListUIState> =
        retryProcessor.flatMap { playListRequest }
            .map { resultState ->
                when (resultState) {
                    is ResultState.Error -> PlayerListUIState.Error(resultState.throwable)
                    is ResultState.Loading -> PlayerListUIState.Loading
                    is ResultState.Success -> PlayerListUIState.Tracks(resultState.data.sortedBy { it.ordinal })
                }
            }

    fun retryLoadingPlaylist() {
        retryProcessor.onNext(Unit)

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
