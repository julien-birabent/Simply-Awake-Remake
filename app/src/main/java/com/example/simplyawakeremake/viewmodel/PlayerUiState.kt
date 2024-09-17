package com.example.simplyawakeremake.viewmodel

import androidx.media3.common.Player
import com.example.simplyawakeremake.UiTrack

sealed interface PlayerUIState {
    data class ReadyToPlay(val track: UiTrack, val player: Player) : PlayerUIState
    data object Loading : PlayerUIState
    data object Error : PlayerUIState
}
