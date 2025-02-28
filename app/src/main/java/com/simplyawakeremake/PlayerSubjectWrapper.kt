package com.simplyawakeremake

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PlayerSubjectWrapper(private val player: Player) : Player.Listener {

    private val _playerFlow = MutableSharedFlow<Player>(replay = 0, extraBufferCapacity = 1)
    val playerUpdates: SharedFlow<Player> = _playerFlow

    override fun onPlaybackStateChanged(playbackState: Int) {
        _playerFlow.tryEmit(player)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _playerFlow.tryEmit(player)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _playerFlow.tryEmit(player)
    }

    override fun onPlayerError(error: PlaybackException) {
    }
}