package com.simplyawakeremake.ui.playback

import androidx.media3.common.Player
import com.simplyawakeremake.data.track.Track

sealed interface PlayerUIState {
    data object Loading : PlayerUIState
    data object Error : PlayerUIState

    data class ReadyToPlay(val track: Track) : PlayerUIState
}
