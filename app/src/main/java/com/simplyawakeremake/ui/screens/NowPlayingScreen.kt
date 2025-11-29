package com.simplyawakeremake.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.simplyawakeremake.ui.ToolbarConfig
import com.simplyawakeremake.viewmodel.MainViewModel
import com.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.simplyawakeremake.viewmodel.PlayerListUIState
import com.simplyawakeremake.viewmodel.PlayerUIState
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

    LaunchedEffect(viewModel) {
        viewModel.setupTrackId(trackId)
    }

    val isPlayingState by viewModel.isPlaying.collectAsState(false)
    val totalDurationState by viewModel.totalDurationInMs.collectAsState(initial = 0L)
    val currentPositionState by viewModel.playerPositionUpdates.collectAsState(0L)
    val uiState by viewModel.uiState.collectAsState(initial = PlayerUIState.Loading)

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
                    val error = (uiState as PlayerListUIState.Error).throwable
                    CommonErrorView(throwable = error)
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
                        TrackInformationSection(track.displayName, track.tagString)
                        Spacer(modifier = Modifier.size(12.dp))
                        PlayerControlsView(
                            exoPlayer = readyState.player,
                            totalDuration = totalDurationState,
                            currentPosition = currentPositionState,
                            isPlaying = isPlayingState
                        ) { controlButtons ->
                            viewModel.onControlPressed(controlButtons)
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSlider(player: Player, duration: Long) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting by remember { derivedStateOf { isPressed || isDragged } }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0 && !isInteracting) {
                val currentPosition = player.currentPosition.toFloat()
                sliderPosition = currentPosition
                    .div(player.duration)
                    .times(100f)
            }
            delay(1000L)
        }
    }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { newSliderPosition -> sliderPosition = newSliderPosition },
            onValueChangeFinished = {
                val newPosition = (sliderPosition / 100f) * duration
                player.seekTo(newPosition.toLong())
            },
            valueRange = 0f..100f,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TrackInformationSection(trackName: String, tags: String) {
    Column(Modifier.fillMaxWidth()) {
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
}

@Composable
fun PlayerControlsView(
    exoPlayer: Player,
    totalDuration: Long,
    currentPosition: Long,
    isPlaying: Boolean,
    navigateTrack: (ControlButtons) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PlayerSlider(player = exoPlayer, duration = totalDuration)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentPosition.formatToMinuteAndSeconds(), color = Color.White)
            Text(text = totalDuration.formatToMinuteAndSeconds(), color = Color.White)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, shape = CircleShape)
                    .clip(CircleShape),
                onClick = { navigateTrack(ControlButtons.Play) }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black
                )
            }
        }
    }
}
