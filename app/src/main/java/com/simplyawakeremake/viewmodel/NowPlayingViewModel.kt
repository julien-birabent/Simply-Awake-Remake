package com.simplyawakeremake.viewmodel

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_MEDIA
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.simplyawakeremake.PlayerSubjectWrapper
import com.simplyawakeremake.R
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import com.simplyawakeremake.usecases.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.extensions.toByteArray
import com.simplyawakeremake.service.PlaybackService
import com.simplyawakeremake.ui.model.UiTrack
import com.simplyawakeremake.ui.screens.ControlButtons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
class NowPlayingViewModel(
    private val app: Application,
    trackRepository: TrackRepositoryInterface
) :
    AndroidViewModel(app), KoinComponent {

    private val trackFileManager: TrackFileManager by inject()

    private lateinit var player: Player
    private val trackIdFlow = MutableStateFlow<String?>(null)
    private val playerFlow = MutableStateFlow<Player?>(null)
    private var playerListener: PlayerSubjectWrapper? = null

    private val trackFlow: Flow<ResultState<UiTrack>> = trackIdFlow
        .filterNotNull()
        .flatMapLatest { id -> trackRepository.getTrackBy(id) }
        .flowOn(Dispatchers.IO)

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(1000L)
        }
    }

    val uiState: StateFlow<PlayerUIState> = combine(
        playerFlow.filterNotNull(), trackFlow
    ) { player, resultState ->
        when (resultState) {
            is ResultState.Loading -> PlayerUIState.Loading
            is ResultState.Success -> {
                player.setMediaItem(createMediaItem(resultState.data))
                player.prepare()
                PlayerUIState.ReadyToPlay(resultState.data, player)
            }

            is ResultState.Error -> PlayerUIState.Error
        }
    }
        .catch { error ->
            Log.e(NowPlayingViewModel::class.simpleName, error.message.orEmpty())
            emit(PlayerUIState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, PlayerUIState.Loading)


    private val onPlayerUpdate: Flow<Player> = playerFlow
        .filterNotNull()
        .flatMapLatest { playerListener!!.playerUpdates }
        .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalDurationInMs: StateFlow<Long> = onPlayerUpdate
        .filter { it.playbackState == Player.STATE_READY }
        .map { it.duration }
        .distinctUntilChanged()
        .catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val isPlaying: StateFlow<Boolean> = onPlayerUpdate
        .map { it.isPlaying }
        .distinctUntilChanged()
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val playerPositionUpdates: StateFlow<Long> = combine(tickerFlow, onPlayerUpdate) { _, player ->
        player
    }.filter { it.playbackState == Player.STATE_READY }
        .map { it.currentPosition }
        .catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    init {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                viewModelScope.launch {
                    if (!::player.isInitialized) {
                        player = controllerFuture.get()
                        playerListener = PlayerSubjectWrapper(player)
                        player.addListener(playerListener!!)
                        playerFlow.emit(player)
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun setupTrackId(id: String) {
        trackIdFlow.value = id
    }

    fun onControlPressed(controlPressed: ControlButtons) {
        when (controlPressed) {
            ControlButtons.Play -> {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
    }

    private suspend fun createMediaItem(track: UiTrack): MediaItem = withContext(Dispatchers.IO) {
        val trackUri = trackFileManager.getTrackUri(track.id)

        val mediaMetaData = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(track.displayName)
            .setArtist("Simply Awake : " + track.tagString)
            .setArtworkData(
                AppCompatResources.getDrawable(app, R.drawable.enzo)?.toByteArray(),
                PICTURE_TYPE_MEDIA
            )
            .build()

        return@withContext MediaItem.Builder()
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
