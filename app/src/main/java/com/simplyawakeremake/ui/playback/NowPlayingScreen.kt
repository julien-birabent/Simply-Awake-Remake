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
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.simplyawakeremake.R
import com.simplyawakeremake.extensions.formatToMinuteAndSeconds
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.common.FavoriteButton
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
    val mainViewModel = LocalMainViewModel.current
    val toolbarConfig = ToolbarConfig(showToolbar = false)

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }

    LaunchedEffect(viewModel, trackId) {
        viewModel.setupTrackId(trackId)
        viewModel.onTrackStarted(trackId)
    }

    val isPlayingState by viewModel.isPlaying.collectAsState(false)
    val totalDurationState by viewModel.totalDurationInMs.collectAsState(initial = 0L)
    val currentPositionState by viewModel.playerPositionUpdates.collectAsState(0L)
    val uiState by viewModel.uiState.collectAsState(initial = PlayerUIState.Loading)
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
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
                    // TODO: show error message
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
                            track.displayName,
                            track.tagString,
                            track.isFavorite,
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
fun PlayerSlider(
    duration: Long,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting by remember { derivedStateOf { isPressed || isDragged } }

    var sliderPosition by remember { mutableFloatStateOf(0f) }

    /*val targetSliderPosition by remember(duration, currentPosition) {
        mutableFloatStateOf(
            if (duration > 0L) {
                currentPosition.toFloat()
                    .div(duration.toFloat())
                    .times(100f)
                    .coerceIn(0f, 100f)
            } else {
                0f
            }
        )
    }

    if (!isInteracting) {
        sliderPosition = targetSliderPosition
    }*/

    LaunchedEffect(duration, currentPosition, isInteracting) {
        if (!isInteracting && duration > 0L) {
            val clampedPosition = currentPosition.coerceIn(0L, duration)
            sliderPosition = clampedPosition.toFloat()
                .div(duration.toFloat())
                .times(100f)
                .coerceIn(0f, 100f)
        }
    }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { newSliderPosition ->
                sliderPosition = newSliderPosition
            },
            onValueChangeFinished = {
                val newPosition = (sliderPosition / 100f) * duration
                onSeekTo(newPosition.toLong())
            },
            valueRange = 0f..100f,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
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
fun PlayerControlsView(
    totalDuration: Long,
    currentPosition: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    onControlPressed: (ControlButtons) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PlayerSlider(
            duration = totalDuration,
            currentPosition = currentPosition,
            onSeekTo = onSeekTo
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentPosition.formatToMinuteAndSeconds(), color = Color.White)
            Text(text = totalDuration.formatToMinuteAndSeconds(), color = Color.White)
        }

        Spacer(modifier = Modifier.size(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Spacer(modifier = Modifier.size(24.dp))

            IconButton(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, shape = CircleShape)
                    .clip(CircleShape),
                onClick = { onControlPressed(ControlButtons.Play) }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.size(24.dp))

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
        }

        Spacer(modifier = Modifier.size(12.dp))

        IconButton(
            onClick = { onControlPressed(ControlButtons.ToggleShuffle) }
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else Color.White
            )
        }
    }
}

