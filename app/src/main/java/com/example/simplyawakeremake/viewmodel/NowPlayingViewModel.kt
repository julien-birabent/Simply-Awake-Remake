package com.example.simplyawakeremake.viewmodel

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.example.simplyawakeremake.ExoPlayerSubject
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.ResultState
import com.example.simplyawakeremake.data.track.TrackRepository
import com.example.simplyawakeremake.data.track.TrackUriProvider
import com.example.simplyawakeremake.notifications.SimplyAwakeNotificationManager
import com.example.simplyawakeremake.screens.ControlButtons
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class NowPlayingViewModel(val player: ExoPlayer, private val app: Application) :
    AndroidViewModel(app), KoinComponent {

    private val trackUriProvider: TrackUriProvider by inject()
    private val trackRepository: TrackRepository by inject()
    private val exoPlayerSubject = ExoPlayerSubject(player)
    private val exoPlayerEvents = exoPlayerSubject.observable()

    private val trackIdProcessor: BehaviorProcessor<String> = BehaviorProcessor.create()

    val totalDurationInMs = exoPlayerEvents
        .filter { it.isPlaying }
        .distinctUntilChanged()
        .map { player.duration }

    val isPlaying = exoPlayerEvents.map { it.isPlaying }.distinctUntilChanged()

    private val secondsCounter =
        Flowable.interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())

    // Emit the current position of the playback each seconds in the format of a Long in Milliseconds
    // As long as the player is currently playing
    val playerPositionUpdates =
        Flowable.combineLatest(secondsCounter, exoPlayerEvents) { _, playerStatus ->
            playerStatus
        }
            .filter { it.isPlaying }
            .map { player.currentPosition }
            .subscribeOn(Schedulers.computation())

    init {
        prepareMediaSessionForNotification()
        notificationManager.showNotificationForPlayer(player)
    }

    private lateinit var notificationManager: SimplyAwakeNotificationManager
    private lateinit var mediaSession: MediaSession

    private var isStarted = false

    val uiState: Flowable<PlayerUIState> = trackIdProcessor
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
            addListener(exoPlayerSubject.playerListener)

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

    override fun onCleared() {
        super.onCleared()
        notificationManager.hideNotification()
        player.stop()
        player.removeListener(exoPlayerSubject.playerListener)
        mediaSession.release()
    }

    private fun prepareMediaSessionForNotification() {
        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            app.packageManager?.getLaunchIntentForPackage(app.packageName)
                ?.let { sessionIntent ->
                    PendingIntent.getActivity(
                        app,
                        SESSION_INTENT_REQUEST_CODE,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }

        // Create a new MediaSession.
        mediaSession = MediaSession.Builder(app, player)
            .setSessionActivity(sessionActivityPendingIntent!!)
            .build()

        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager = SimplyAwakeNotificationManager(app, mediaSession.token, player)
    }

    companion object {
        const val SESSION_INTENT_REQUEST_CODE = 654654
    }
}


sealed interface PlayerUIState {
    data class ReadyToPlay(val track: UiTrack) : PlayerUIState
    data object Loading : PlayerUIState
    data object Error : PlayerUIState
}
