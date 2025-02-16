package com.simplyawakeremake

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor

class PlayerSubjectWrapper(private val player: Player):  Player.Listener {

    private val subject: BehaviorProcessor<Player> = BehaviorProcessor.create()

    fun playerUpdates(): Flowable<Player> = subject.share()

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        subject.onNext(player)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        subject.onNext(player)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        subject.onNext(player)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        subject.onError(error)
    }
}