package com.example.simplyawakeremake.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.example.simplyawakeremake.R
import com.example.simplyawakeremake.extensions.formatToMinuteAndSeconds
import com.example.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import com.example.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.example.simplyawakeremake.viewmodel.PlayerUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NowPlayingScreen(
    navController: NavController,
    trackId: String,
    viewModel: NowPlayingViewModel = koinViewModel()
) {

    LaunchedEffect(viewModel) {
        viewModel.setupTrackId(trackId)
    }
    val isPlayingState by viewModel.isPlaying.subscribeAsState(false)
    val totalDurationState by viewModel.totalDurationInMs.subscribeAsState(initial = 0L)
    val currentPositionState by viewModel.playerPositionUpdates.subscribeAsState(0L)

    val uiState by viewModel.uiState.subscribeAsState(initial = PlayerUIState.Loading)


    when (uiState) {
        PlayerUIState.Error -> {

        }

        PlayerUIState.Loading -> {
            LoadingIndicator()
        }

        is PlayerUIState.ReadyToPlay -> {
            val track = (uiState as PlayerUIState.ReadyToPlay).track
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.enzo),
                    contentDescription = "Description of the image",
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.5f, false),
                    contentScale = ContentScale.Fit
                )
                TrackInformationSection(track.name, track.tagString)
                Spacer(modifier = Modifier.size(12.dp))
                PlayerControlsView(
                    viewModel.player,
                    totalDurationState,
                    currentPositionState,
                    isPlayingState
                ) { controlButtons -> viewModel.onControlPressed(controlButtons) }
                Spacer(modifier = Modifier.size(12.dp))
            }
        }
    }

}

@Composable
fun PlayerSlider(exoPlayer: ExoPlayer, duration: Long) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    // Coroutine to update the slider position
    LaunchedEffect(exoPlayer) {
            while (true) {
                // Check if the player is ready and playing
                if (exoPlayer.isPlaying && duration > 0 && !isInteracting) {
                    val currentPosition = exoPlayer.currentPosition.toFloat()
                    sliderPosition = currentPosition.div(duration).times(100f) // Normalize the position between 0 and 100
                }
                delay(1000L) // Update every second
            }
    }
    Column {
        // Slider to reflect and control playback position
        Slider(
            value = sliderPosition,
            onValueChange = { newSliderPosition -> sliderPosition = newSliderPosition },
            onValueChangeFinished = {
                // When the user finishes sliding, seek to the new position in the ExoPlayer
                val newPosition = (sliderPosition / 100) * duration
                exoPlayer.seekTo(newPosition.toLong())
            },
            valueRange = 0f..100f, // Slider range is normalized from 0 to 100,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NowPlayingPreview() {
    SimplyAwakeRemakeTheme {
        NowPlayingScreen(
            navController = NavController(context = LocalContext.current),
            trackId = ""
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

/**
 * Composable function to display the player controls.
 * This composable displays controls for managing audio playback, including a slider for track progress,
 * buttons for rewinding, playing/pausing, and skipping tracks, and current track information.
 * @param totalDuration The total duration of the current track.
 * @param currentPosition The current position within the track.
 * @param isPlaying Whether the track is currently playing or paused.
 * @param navigateTrack Function to navigate to the next or previous track.
 * @param seekPosition Function to seek to a specific position within the track.
 */
@Composable
fun PlayerControlsView(
    exoPlayer: ExoPlayer,
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
        PlayerSlider(exoPlayer = exoPlayer, totalDuration)

        // Display current position and total duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentPosition.formatToMinuteAndSeconds(), color = Color.White)
            Text(text = totalDuration.formatToMinuteAndSeconds(), color = Color.White)
        }

        // Row for control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Play/pause button
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