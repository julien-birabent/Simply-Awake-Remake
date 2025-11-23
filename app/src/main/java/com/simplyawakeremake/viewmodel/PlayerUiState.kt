package com.simplyawakeremake.viewmodel

import androidx.media3.common.Player
import com.simplyawakeremake.ui.model.Track

sealed interface PlayerUIState {
    data class ReadyToPlay(val track: Track, val player: Player) : PlayerUIState
    data object Loading : PlayerUIState
    data object Error : PlayerUIState
}
