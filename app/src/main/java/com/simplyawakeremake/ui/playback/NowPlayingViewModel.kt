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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    private val registerTrackPlayUseCase: RegisterTrackPlayUseCase
) : AndroidViewModel(app), KoinComponent {

    private val trackFileManager: TrackFileManager by inject()

    private lateinit var player: Player
    private val trackIdFlow = MutableStateFlow<String?>(null)
    private val playerFlow = MutableStateFlow<Player?>(null)
    private var playerListener: PlayerSubjectWrapper? = null

    private var playlistTrackIds: List<String> = emptyList()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val trackFlow: Flow<ResultState<Track>> = trackIdFlow
        .filterNotNull()
        .flatMapLatest { id -> userTrackRepository.getTrackBy(id) }
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
                val track = resultState.data

                val currentMediaId = player.currentMediaItem?.mediaId
                if (currentMediaId != track.id) {
                    player.setMediaItem(createMediaItem(track))
                    player.prepare()
                }

                PlayerUIState.ReadyToPlay(track, player)
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
    }

    private suspend fun ensurePlaylistLoaded() {
        if (playlistTrackIds.isNotEmpty()) return

        val ids: List<String> = userTrackRepository.getAllTracks()
            .firstOrNull { it is ResultState.Success }
            ?.let { result ->
                (result as ResultState.Success).data.map { it.id }
            }
            ?: emptyList()

        playlistTrackIds = ids
    }

    fun setupTrackId(id: String) {
        trackIdFlow.value = id
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

        val index = playlistTrackIds.indexOf(currentId)
        if (index == -1) return null
        val prevIndex = if (index == 0) playlistTrackIds.lastIndex else index - 1
        return playlistTrackIds[prevIndex]
    }

    fun onControlPressed(controlPressed: ControlButtons) {
        when (controlPressed) {
            ControlButtons.Play -> {
                if (player.isPlaying) player.pause() else player.play()
            }

            ControlButtons.Next -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val currentId = trackIdFlow.value ?: return@launch
                    val nextId = getNextTrackId(currentId) ?: return@launch
                    trackIdFlow.emit(nextId)
                    onTrackStarted(nextId)
                }
            }

            ControlButtons.Previous -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val currentId = trackIdFlow.value ?: return@launch
                    val prevId = getPreviousTrackId(currentId) ?: return@launch
                    trackIdFlow.emit(prevId)
                    onTrackStarted(prevId)
                }
            }

            ControlButtons.ToggleShuffle -> {
                _isShuffleEnabled.value = !_isShuffleEnabled.value
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

    override fun onCleared() {
        super.onCleared()
        player.stop()
        playerListener?.let { player.removeListener(it) }
    }
}
