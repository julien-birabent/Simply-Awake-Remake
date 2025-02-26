package com.simplyawakeremake.viewmodel

import androidx.media3.common.Player
import com.simplyawakeremake.data.track.UiTrack

sealed interface PlayerUIState {
    data class ReadyToPlay(val track: UiTrack, val player: Player) : PlayerUIState
    data object Loading : PlayerUIState
    data object Error : PlayerUIState
}
