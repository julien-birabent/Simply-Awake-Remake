package com.simplyawakeremake.ui.playback

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
import com.simplyawakeremake.data.playback.PlaybackPreferencesRepository
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.extensions.toByteArray
import com.simplyawakeremake.service.PlaybackService
import com.simplyawakeremake.usecases.RegisterTrackPlayUseCase
import com.simplyawakeremake.usecases.ToggleTrackFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    private val userTrackRepository: TrackRepositoryInterface,
    private val toggleTrackFavoriteUseCase: ToggleTrackFavoriteUseCase,
    private val registerTrackPlayUseCase: RegisterTrackPlayUseCase,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : AndroidViewModel(app), KoinComponent {

    private val TAG = "NowPlayingViewModel"
    private val trackFileManager: TrackFileManager by inject()

    private lateinit var player: Player
    private val playerFlow = MutableStateFlow<Player?>(null)
    private var playerListener: PlayerSubjectWrapper? = null

    private val trackIdFlow = MutableStateFlow<String?>(null)
    private var playlistTrackIds: List<String> = emptyList()

    private val playbackHistory = mutableListOf<String>()

    val immersiveModeEnabled: StateFlow<Boolean> =
        playbackPreferencesRepository.immersiveModeEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true
            )

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled


    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled

    private val _isAutoPlayNextEnabled = MutableStateFlow(false)
    val isAutoPlayNextEnabled: StateFlow<Boolean> = _isAutoPlayNextEnabled

    private data class PlaybackRequest(
        val trackId: String,
        val autoPlay: Boolean
    )

    private val playbackRequestFlow = MutableSharedFlow<PlaybackRequest>(
        extraBufferCapacity = 1
    )

    private val trackFlow: Flow<ResultState<Track>> = trackIdFlow
        .filterNotNull()
        .flatMapLatest { id -> userTrackRepository.getTrackBy(id) }
        .flowOn(Dispatchers.IO)

    val uiState: StateFlow<PlayerUIState> = trackFlow
        .map { resultState ->
            when (resultState) {
                is ResultState.Loading -> PlayerUIState.Loading
                is ResultState.Error -> PlayerUIState.Error
                is ResultState.Success -> PlayerUIState.ReadyToPlay(resultState.data)
            }
        }
        .catch { error ->
            Log.e(NowPlayingViewModel::class.simpleName, error.message.orEmpty())
            emit(PlayerUIState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, PlayerUIState.Loading)

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(1000L)
        }
    }

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

    val isBufferingAudio: StateFlow<Boolean> = onPlayerUpdate
        .map { player ->
            player.playbackState == Player.STATE_BUFFERING
        }
        .distinctUntilChanged()
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)


    val playerPositionUpdates: StateFlow<Long> = combine(tickerFlow, onPlayerUpdate) { _, player ->
        player
    }
        .filter { it.playbackState == Player.STATE_READY }
        .map { it.currentPosition }
        .catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    init {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        viewModelScope.launch(Dispatchers.IO) { ensurePlaylistLoaded() }

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

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                playerFlow.filterNotNull(),
                playbackRequestFlow.distinctUntilChanged()
            ) { _, request ->
                request
            }.collectLatest { request ->
                Log.d(TAG, "Receiving new playback request: $request")
                loadTrackForPlayback(request.trackId, request.autoPlay)
            }
        }

        viewModelScope.launch {
            onPlayerUpdate
                .map { it.playbackState }
                .distinctUntilChanged()
                .filter { it == Player.STATE_ENDED }
                .collect {
                    handleTrackEnded()
                }
        }
    }

    private suspend fun ensurePlaylistLoaded() {
        if (playlistTrackIds.isNotEmpty()) return

        val ids: List<String> = userTrackRepository.getAllTracks()
            .firstOrNull { it is ResultState.Success }
            ?.let { result ->
                (result as ResultState.Success).data.map { it.id }
            }
            ?: emptyList()

        Log.d(TAG, "Loading playlist track ids = ${ids.size}")
        playlistTrackIds = ids
    }

    private fun pushToHistory(trackId: String) {
        if (playbackHistory.lastOrNull() != trackId) {
            playbackHistory.add(trackId)
        }
    }

    private suspend fun switchToTrack(
        trackId: String,
        autoPlay: Boolean
    ) {
        trackIdFlow.emit(trackId)
        pushToHistory(trackId)
        playbackRequestFlow.emit(
            PlaybackRequest(
                trackId = trackId,
                autoPlay = autoPlay
            )
        )
        onTrackStarted(trackId)
    }

    fun setupTrackId(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            switchToTrack(
                trackId = id,
                autoPlay = true
            )
        }
    }

    private fun getNextTrackId(currentId: String): String? {
        if (playlistTrackIds.isEmpty()) return null

        return if (_isShuffleEnabled.value) {
            val candidates = playlistTrackIds.filterNot { it == currentId }
            (candidates.ifEmpty { playlistTrackIds }).randomOrNull()
        } else {
            val index = playlistTrackIds.indexOf(currentId)
            if (index == -1) return null
            val nextIndex = (index + 1) % playlistTrackIds.size
            playlistTrackIds[nextIndex]
        }
    }

    private fun getPreviousTrackId(currentId: String): String? {
        if (playlistTrackIds.isEmpty()) return null

        return if (_isShuffleEnabled.value) {
            if (playbackHistory.size < 2) {
                null
            } else {
                if (playbackHistory.last() == currentId) {
                    playbackHistory.removeAt(playbackHistory.lastIndex)
                }
                playbackHistory.lastOrNull()
            }
        } else {
            val index = playlistTrackIds.indexOf(currentId)
            if (index == -1) return null
            val prevIndex = if (index == 0) playlistTrackIds.lastIndex else index - 1
            playlistTrackIds[prevIndex]
        }
    }

    fun onControlPressed(controlPressed: ControlButtons) {
        when (controlPressed) {
            ControlButtons.Play -> {
                if (!::player.isInitialized) return
                if (player.isPlaying) player.pause() else player.play()
            }

            ControlButtons.Next -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val currentId = trackIdFlow.value ?: return@launch
                    val nextId = getNextTrackId(currentId) ?: return@launch

                    switchToTrack(
                        trackId = nextId,
                        autoPlay = true,
                    )
                }
            }

            ControlButtons.Previous -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val currentId = trackIdFlow.value ?: return@launch
                    val prevId = getPreviousTrackId(currentId) ?: return@launch

                    switchToTrack(
                        trackId = prevId,
                        autoPlay = true,
                    )
                }
            }

            ControlButtons.ToggleShuffle -> {
                _isShuffleEnabled.value = !_isShuffleEnabled.value
            }

            ControlButtons.ToggleRepeat -> {
                _isRepeatEnabled.value = !_isRepeatEnabled.value
            }

            ControlButtons.ToggleAutoPlayNext -> {
                _isAutoPlayNextEnabled.value = !_isAutoPlayNextEnabled.value
            }

        }
    }

    private suspend fun loadTrackForPlayback(trackId: String, autoPlay: Boolean) = withContext(
        Dispatchers.IO
    ) {
        val trackResult = userTrackRepository.getTrackBy(trackId).firstOrNull()
        val track = (trackResult as? ResultState.Success)?.data ?: return@withContext

        Log.d(TAG, "Loading track for playback = $track")
        val mediaItem = createMediaItem(track)

        withContext(Dispatchers.Main) {
            if (!::player.isInitialized) return@withContext

            val wasPlaying = player.isPlaying

            player.setMediaItem(mediaItem)
            player.prepare()

            val shouldPlay = autoPlay || wasPlaying
            if (shouldPlay) {
                player.play()
            }
        }
    }

    private suspend fun createMediaItem(track: Track): MediaItem = withContext(Dispatchers.IO) {
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

    fun onFavoriteClicked(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleTrackFavoriteUseCase(track)
        }
    }

    fun onTrackStarted(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            registerTrackPlayUseCase(trackId)
        }
    }

    fun onSeekTo(positionMs: Long) {
        if (!::player.isInitialized) return
        player.seekTo(positionMs)
    }

    private fun handleTrackEnded() {
        val currentId = trackIdFlow.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            when {
                _isRepeatEnabled.value -> {
                    Log.d(TAG, "Track ended → repeating current track: $currentId")
                    withContext(Dispatchers.Main) {
                        if (::player.isInitialized) {
                            player.seekTo(0L)
                            player.playWhenReady = true
                        }
                    }
                }

                _isAutoPlayNextEnabled.value -> {
                    val nextId = getNextTrackId(currentId)
                    if (nextId != null) {
                        Log.d(TAG, "Track ended → auto-playing next track: $nextId")
                        switchToTrack(
                            trackId = nextId,
                            autoPlay = true
                        )
                    } else {
                        Log.d(TAG, "Track ended → no next track found, staying ended")
                    }
                }

                // 3) Nothing else: just stop at the end
                else -> {
                    Log.d(TAG, "Track ended → repeat OFF & auto-play-next OFF, staying ended")
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        if (::player.isInitialized) {
            player.stop()
            playerListener?.let { player.removeListener(it) }
        }
    }
}
