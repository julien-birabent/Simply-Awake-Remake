package com.example.simplyawakeremake

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor

class ExoPlayerPublishSubject(private val exoPlayer: ExoPlayer) {

    private val subject: BehaviorProcessor<PlayerStatus> = BehaviorProcessor.create()

    fun observable(): Flowable<PlayerStatus> = subject

    val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            subject.onNext(PlayerStatus(exoPlayer.isPlaying, playbackState))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            subject.onNext(PlayerStatus(exoPlayer.isPlaying, exoPlayer.playbackState))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            subject.onNext(PlayerStatus(isPlaying, exoPlayer.playbackState))
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            subject.onError(error)
        }
    }
}

data class PlayerStatus(val isPlaying: Boolean, val playbackState: Int)