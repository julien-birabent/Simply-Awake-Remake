package com.simplyawakeremake.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_MEDIA
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.simplyawakeremake.PlayerSubjectWrapper
import com.simplyawakeremake.R
import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepository
import com.simplyawakeremake.data.track.TrackUriProvider
import com.simplyawakeremake.extensions.toByteArray
import com.simplyawakeremake.screens.ControlButtons
import com.simplyawakeremake.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

@UnstableApi
class NowPlayingViewModel(private val app: Application) :
    AndroidViewModel(app), KoinComponent {

    private val trackUriProvider: TrackUriProvider by inject()
    private val trackRepository: TrackRepository by inject()

    private lateinit var player: Player
    private val trackIdProcessor: BehaviorProcessor<String> = BehaviorProcessor.create()
    private val playerProcessor: BehaviorProcessor<Player> = BehaviorProcessor.create<Player>()
    private var playerListener: PlayerSubjectWrapper? = null

    private val getTrackRequest = trackIdProcessor.flatMap { trackRepository.getTrackBy(it) }

    val uiState: Flowable<PlayerUIState> =
        Flowable.combineLatest(playerProcessor.share(), getTrackRequest) { player, requestResults ->
            player to requestResults
        }.map { (player, resultState) ->
            when (resultState) {
                is ResultState.Loading -> {
                    PlayerUIState.Loading
                }

                is ResultState.Success -> {
                    player.setMediaItem(createMediaItem(resultState.data))
                    player.prepare()
                    PlayerUIState.ReadyToPlay(resultState.data, player)
                }

                is ResultState.Error -> {
                    PlayerUIState.Error
                }
            }
        }.share()

    private val onPlayerUpdate = playerProcessor
        .flatMap { playerListener!!.playerUpdates() }
        .share()

    val totalDurationInMs = onPlayerUpdate
        .filter { it.playbackState == Player.STATE_READY }
        .map { it.duration }
        .distinctUntilChanged()

    val isPlaying = onPlayerUpdate.map { it.isPlaying }.distinctUntilChanged()

    private val secondsCounter =
        Flowable.interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())

    // Emit the current position of the playback each seconds in the format of a Long in Milliseconds
    // As long as the player is currently playing
    val playerPositionUpdates =
        Flowable.combineLatest(secondsCounter, onPlayerUpdate) { _, player ->
            player
        }
            .filter { it.playbackState == Player.STATE_READY }
            .map { it.currentPosition }
            .subscribeOn(Schedulers.computation())

    init {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                if (!::player.isInitialized) {
                    player = controllerFuture.get()
                    playerListener = PlayerSubjectWrapper(player)
                    player.addListener(playerListener!!)
                    playerProcessor.onNext(player)
                }
            },
            MoreExecutors.directExecutor()
        )
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
    private fun createMediaItem(track: UiTrack): MediaItem {
        val mediaMetaData = MediaMetadata.Builder()
            .setTitle(track.displayName)
            .setArtist("Simply Awake : " + track.tagString)
            .setArtworkData(
                AppCompatResources.getDrawable(app, R.drawable.enzo)?.toByteArray(),
                PICTURE_TYPE_MEDIA
            )
            .build()

        val trackUri = Uri.parse(trackUriProvider.trackUri(track.id))
        return MediaItem.Builder()
            .setUri(trackUri)
            .setMediaId(track.id)
            .setMediaMetadata(mediaMetaData)
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        player.stop()
        playerListener?.let { player.removeListener(it) }
    }
}
