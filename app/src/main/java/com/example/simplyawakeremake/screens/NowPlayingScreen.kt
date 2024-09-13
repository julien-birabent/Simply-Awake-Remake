package com.example.simplyawakeremake.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava3.subscribeAsState
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
import androidx.navigation.NavController
import com.example.simplyawakeremake.R
import com.example.simplyawakeremake.extensions.fromMsToMinuteSeconds
import com.example.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import com.example.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.example.simplyawakeremake.viewmodel.PlayerUIState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun NowPlayingScreen(
    navController: NavController,
    trackId: String,
    viewModel: NowPlayingViewModel = koinViewModel()
) {

    LaunchedEffect(viewModel) {
        viewModel.setupTrackId(trackId)
    }
    val isPlayingState by viewModel.isPlaying.collectAsStateWithLifecycle()
    val totalDurationState by viewModel.totalDurationInMS.collectAsStateWithLifecycle()
    var currentPositionState by remember { mutableLongStateOf(0L) }

    val uiState by viewModel.uiState.subscribeAsState(initial = PlayerUIState.Loading)

    LaunchedEffect(isPlayingState) {
        while (isPlayingState) {
            currentPositionState = viewModel.player.currentPosition
            delay(1.seconds)
        }
    }

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
                    totalDurationState,
                    currentPositionState,
                    isPlayingState,
                    { controlButtons -> viewModel.onControlPressed(controlButtons) },
                    { position -> viewModel.updatePlayerPosition((position * 1000).toLong()) },
                    modifier = Modifier.weight(1f, false)
                )
            }
        }
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
    totalDuration: Long,
    currentPosition: Long,
    isPlaying: Boolean,
    navigateTrack: (ControlButtons) -> Unit,
    seekPosition: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Slider for track progress
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = (currentPosition / 1000).toFloat(),
            valueRange = 0f..(totalDuration / 1000).toFloat(),
            onValueChange = { seekPosition(it) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTickColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = Color.White
            )
        )

        // Display current position and total duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentPosition.fromMsToMinuteSeconds(), color = Color.White)
            Text(text = totalDuration.fromMsToMinuteSeconds(), color = Color.White)
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