package com.simplyawakeremake.ui.playback

import com.simplyawakeremake.data.track.Track

sealed interface PlayerUIState {
    data object Loading : PlayerUIState
    data object Error : PlayerUIState

    data class ReadyToPlay(val track: Track) : PlayerUIState
}
