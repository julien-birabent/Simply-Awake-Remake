package com.example.simplyawakeremake.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.example.simplyawakeremake.ExoPlayerPublishSubject
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.ResultState
import com.example.simplyawakeremake.data.track.TrackRepository
import com.example.simplyawakeremake.data.track.TrackUriProvider
import com.example.simplyawakeremake.screens.ControlButtons
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class NowPlayingViewModel(val player: ExoPlayer, private val app: Application) :
    AndroidViewModel(app), KoinComponent {

    private val trackUriProvider: TrackUriProvider by inject()
    private val trackRepository: TrackRepository by inject()
    private val exoPlayerPublishSubject = ExoPlayerPublishSubject(player)
    private val exoPlayerEvents = exoPlayerPublishSubject.observable()

    private val trackIdProcessor: BehaviorProcessor<String> =
        BehaviorProcessor.create()

    val totalDurationInMs = exoPlayerEvents.filter { it.isPlaying }
            .distinctUntilChanged()
            .map { player.duration }

    val isPlaying = exoPlayerEvents.map { it.isPlaying }.distinctUntilChanged()

    // Emit the current position of the playback each seconds in the format of a Long in Milliseconds
    val playerPositionUpdates =
        Flowable.combineLatest(Flowable.interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread()), exoPlayerEvents) { _, playerStatus ->
            playerStatus
        }
            .filter { it.isPlaying }
            .map { player.currentPosition }
            .subscribeOn(Schedulers.computation())


    //private lateinit var notificationManager: SimplyAwakeNotificationManager

    private lateinit var mediaSession: MediaSession

    private var isStarted = false

    val uiState: Flowable<PlayerUIState> =
        trackIdProcessor
            .flatMap { trackRepository.getTrackBy(it) }
            .takeUntil { it !is ResultState.Loading }
            .map { resultState ->
                when (resultState) {
                    is ResultState.Loading -> {
                        PlayerUIState.Loading
                    }

                    is ResultState.Success -> {
                        preparePlayer(resultState.data)
                        PlayerUIState.ReadyToPlay(resultState.data)
                    }

                    is ResultState.Error -> {
                        PlayerUIState.Error
                    }
                }
            }

    fun setupTrackId(id: String) {
        trackIdProcessor.onNext(id)
    }

    fun onControlPressed(controlPressed: ControlButtons) {
        when (controlPressed) {
            ControlButtons.Play -> {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun preparePlayer(track: UiTrack) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player.apply {
            setAudioAttributes(audioAttributes, true)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(exoPlayerPublishSubject.playerListener)

            playWhenReady = true
            setMediaSource(createMediaSourceFrom(app, track))
            prepare()
        }
    }

    @OptIn(UnstableApi::class)
    private fun createMediaSourceFrom(context: Context, track: UiTrack): ProgressiveMediaSource {
        val mediaMetaData = MediaMetadata.Builder()
            .setTitle(track.name)
            .setAlbumArtist("Simply Awake")
            .build()

        val trackUri = Uri.parse(trackUriProvider.trackUri(track.id))
        val mediaItem = MediaItem.Builder()
            .setUri(trackUri)
            .setMediaId(track.id)
            .setMediaMetadata(mediaMetaData)
            .build()
        val dataSourceFactory = DefaultDataSource.Factory(context)

        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    fun updatePlayerPosition(position: Long) {
        player.seekTo(position)
    }

}


sealed interface PlayerUIState {
    data class ReadyToPlay(val track: UiTrack) : PlayerUIState
    data object Loading : PlayerUIState
    data object Error : PlayerUIState
}
