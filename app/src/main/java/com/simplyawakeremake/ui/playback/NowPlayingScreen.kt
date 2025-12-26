package com.simplyawakeremake.ui.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.simplyawakeremake.R
import com.simplyawakeremake.extensions.formatToMinuteAndSeconds
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.common.FavoriteButton
import com.simplyawakeremake.ui.common.ImmersiveMode
import com.simplyawakeremake.ui.common.LoadingIndicator
import com.simplyawakeremake.ui.common.ToolbarConfig
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableApi::class)
@Composable
fun NowPlayingScreen(
    navController: NavController,
    trackId: String,
    viewModel: NowPlayingViewModel = koinViewModel()
) {
    val immersiveEnabled by viewModel.immersiveModeEnabled.collectAsState(initial = true)
    ImmersiveMode(immersiveEnabled)

    val mainViewModel = LocalMainViewModel.current
    val toolbarConfig = ToolbarConfig(showToolbar = false)

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }

    LaunchedEffect(viewModel, trackId) {
        viewModel.setupTrackId(trackId)
    }

    val isPlayingState by viewModel.isPlaying.collectAsState(false)
    val totalDurationState by viewModel.totalDurationInMs.collectAsState(initial = 0L)
    val currentPositionState by viewModel.playerPositionUpdates.collectAsState(0L)
    val uiState by viewModel.uiState.collectAsState(initial = PlayerUIState.Loading)

    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsState()
    val isAutoPlayNextEnabled by viewModel.isAutoPlayNextEnabled.collectAsState()
    val isBufferingAudio by viewModel.isBufferingAudio.collectAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (uiState) {
                PlayerUIState.Error -> {
                    // TODO: show a proper error view
                }

                PlayerUIState.Loading -> {
                    LoadingIndicator()
                }

                is PlayerUIState.ReadyToPlay -> {
                    val readyState = uiState as PlayerUIState.ReadyToPlay
                    val track = readyState.track

                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.enzo),
                            contentDescription = "Track artwork",
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.5f, fill = false),
                            contentScale = ContentScale.Fit
                        )

                        TrackInformationSection(
                            trackName = track.displayName,
                            tags = track.tagString,
                            isFavorite = track.isFavorite,
                            onFavoriteClicked = {
                                viewModel.onFavoriteClicked(track)
                            }
                        )

                        Spacer(modifier = Modifier.size(12.dp))

                        PlayerControlsView(
                            totalDuration = totalDurationState,
                            currentPosition = currentPositionState,
                            isPlaying = isPlayingState,
                            isShuffleEnabled = isShuffleEnabled,
                            isRepeatEnabled = isRepeatEnabled,
                            isAutoPlayNextEnabled = isAutoPlayNextEnabled,
                            isBufferingAudio = isBufferingAudio,
                            onControlPressed = viewModel::onControlPressed,
                            onSeekTo = viewModel::onSeekTo
                        )

                        Spacer(modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackInformationSection(
    trackName: String,
    tags: String,
    isFavorite: Boolean = false,
    onFavoriteClicked: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = trackName,
                color = Color.White,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = tags,
                color = Color.White,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
        }
        FavoriteButton(
            modifier = Modifier
                .requiredSize(32.dp)
                .align(Alignment.Top),
            iconSize = 32.dp,
            isFavorite = isFavorite,
            onClick = onFavoriteClicked
        )
    }
}

@Composable
fun PlayerSlider(
    duration: Long,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit,
    onSeekPreview: (Long?) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting by remember { derivedStateOf { isPressed || isDragged } }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var pendingSeekTargetMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(duration, currentPosition, isInteracting, pendingSeekTargetMs) {
        if (duration <= 0L) return@LaunchedEffect

        when {
            // User has their finger on it → we don't touch sliderPosition
            isInteracting -> {
                // no-op
            }

            // We've just requested a seek; keep the thumb at the target
            // until the player position gets close to that target.
            pendingSeekTargetMs != null -> {
                val target = pendingSeekTargetMs!!
                sliderPosition = (target.toFloat() / duration.toFloat() * 100f)
                    .coerceIn(0f, 100f)

                val diff = kotlin.math.abs(currentPosition - target)
                if (diff <= 250L) { // ~250ms tolerance
                    pendingSeekTargetMs = null
                }
            }

            // Normal mode: follow the real playback position
            else -> {
                val clampedPosition = currentPosition.coerceIn(0L, duration)
                sliderPosition = (clampedPosition.toFloat() / duration.toFloat() * 100f)
                    .coerceIn(0f, 100f)
            }
        }
    }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { newSliderPosition ->
                sliderPosition = newSliderPosition

                if (duration > 0L) {
                    val previewPosition = (newSliderPosition / 100f) * duration
                    onSeekPreview(previewPosition.toLong())
                } else {
                    onSeekPreview(null)
                }
            },
            onValueChangeFinished = {
                if (duration > 0L) {
                    val finalPosition = (sliderPosition / 100f) * duration
                    val finalPositionMs = finalPosition.toLong()

                    // keep slider visually at the target until the player catches up
                    pendingSeekTargetMs = finalPositionMs

                    // do NOT clear preview here – PlayerControlsView handles it
                    onSeekTo(finalPositionMs)
                } else {
                    onSeekPreview(null)
                    pendingSeekTargetMs = null
                }
            },
            valueRange = 0f..100f,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PlayerControlsView(
    totalDuration: Long,
    currentPosition: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    isAutoPlayNextEnabled: Boolean,
    onControlPressed: (ControlButtons) -> Unit,
    onSeekTo: (Long) -> Unit,
    isBufferingAudio: Boolean
) {
    var previewPositionMs by remember { mutableStateOf<Long?>(null) }

    // If user is scrubbing or we have a pending seek target → show preview time.
    // Otherwise show real playback time.
    val displayedPositionMs = previewPositionMs ?: currentPosition

    // Clear preview once the player has caught up to the seek target
    LaunchedEffect(currentPosition, previewPositionMs) {
        val target = previewPositionMs ?: return@LaunchedEffect
        val diff = kotlin.math.abs(currentPosition - target)
        if (diff <= 250L) {
            previewPositionMs = null
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PlayerSlider(
            duration = totalDuration,
            currentPosition = currentPosition,
            onSeekTo = onSeekTo,
            onSeekPreview = { newPreviewPosition ->
                previewPositionMs = newPreviewPosition
            }
        )

        PlaybackTimeRow(
            currentPositionMs = displayedPositionMs,
            totalDurationMs = totalDuration
        )

        Spacer(modifier = Modifier.size(16.dp))

        PlaybackButtonsRow(
            isPlaying = isPlaying,
            isShuffleEnabled = isShuffleEnabled,
            isRepeatEnabled = isRepeatEnabled,
            onControlPressed = onControlPressed,
            isBufferingAudio = isBufferingAudio
        )

        Spacer(modifier = Modifier.size(12.dp))

        AutoPlayToggle(
            isEnabled = isAutoPlayNextEnabled,
            onToggle = { onControlPressed(ControlButtons.ToggleAutoPlayNext) }
        )
    }
}

@Composable
private fun PlaybackTimeRow(
    currentPositionMs: Long,
    totalDurationMs: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentPositionMs.formatToMinuteAndSeconds(),
            color = Color.White
        )
        Text(
            text = totalDurationMs.formatToMinuteAndSeconds(),
            color = Color.White
        )
    }
}

@Composable
private fun PlaybackToggleIcon(
    icon: ImageVector,
    contentDescription: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}

@Composable
private fun PlaybackButtonsRow(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    onControlPressed: (ControlButtons) -> Unit,
    isBufferingAudio: Boolean
) {
    val repeatIcon = if (isRepeatEnabled) Icons.Filled.RepeatOn else Icons.Filled.Repeat
    val shuffleIcon = if (isShuffleEnabled) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle
    val showBufferingIndicator = rememberDelayedBuffering(isBufferingAudio, 250L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackToggleIcon(
            icon = repeatIcon,
            contentDescription = "Repeat current track",
            isEnabled = isRepeatEnabled,
            onClick = { onControlPressed(ControlButtons.ToggleRepeat) }
        )

        Spacer(modifier = Modifier.size(12.dp))

        IconButton(
            onClick = { onControlPressed(ControlButtons.Previous) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.size(16.dp))

        IconButton(
            modifier = Modifier
                .size(64.dp)
                .background(Color.White, shape = CircleShape)
                .clip(CircleShape),
            enabled = !showBufferingIndicator,
            onClick = { onControlPressed(ControlButtons.Play) }
        ) {
            if (showBufferingIndicator) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        IconButton(
            onClick = { onControlPressed(ControlButtons.Next) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        PlaybackToggleIcon(
            icon = shuffleIcon,
            contentDescription = "Shuffle",
            isEnabled = isShuffleEnabled,
            onClick = { onControlPressed(ControlButtons.ToggleShuffle) }
        )
    }
}

@Composable
private fun rememberDelayedBuffering(
    isBuffering: Boolean,
    delayMs: Long = 250L
): Boolean {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(isBuffering) {
        if (isBuffering) {
            delay(delayMs)
            show = isBuffering
        } else {
            show = false
        }
    }
    return show
}


@Composable
private fun AutoPlayToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackToggleIcon(
            icon = ImageVector.vectorResource(id = R.drawable.ic_autoplay_24dp),
            contentDescription = "Auto play next",
            isEnabled = isEnabled,
            onClick = onToggle
        )
    }
}
