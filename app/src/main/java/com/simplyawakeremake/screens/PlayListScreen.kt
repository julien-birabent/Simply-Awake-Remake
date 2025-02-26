package com.simplyawakeremake.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.data.track.UiTrack
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.viewmodel.PlayerListUIState
import com.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.androidx.compose.koinViewModel
import java.net.UnknownHostException
import com.simplyawakeremake.screens.LoadingIndicator as LoadingIndicator1

@Composable
fun PlayListScreen(navController: NavController, viewModel: TrackListViewModel = koinViewModel()) {

    val uiState by viewModel.screenState.subscribeAsState(initial = PlayerListUIState.Loading)

    when (uiState) {
        is PlayerListUIState.Error -> {
            when (val error = (uiState as PlayerListUIState.Error).throwable) {
                is UnknownHostException -> {
                    NoInternetScreen { viewModel.retryLoadingPlaylist() }
                }

                else -> {
                    CommonErrorView(throwable = error)
                }
            }
        }

        PlayerListUIState.Loading -> {
            LoadingIndicator1()
        }

        is PlayerListUIState.Tracks -> {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Playlist(
                    tracks = (uiState as PlayerListUIState.Tracks).items,
                    navController
                )
            }
        }
    }
}


@Composable
fun Playlist(tracks: List<UiTrack>, navController: NavController) {
    ItemList(
        tracks,
        { index -> tracks[index].id },
        { navController.navigate(Screen.NOW_PLAYING.name + "/${it.id}") },
        divider = { HorizontalDivider(color = Color.White, thickness = 1.dp) },
    ) { track -> TrackItem(track)}
}

@Composable
fun TrackItem(track: UiTrack) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${track.ordinal} " + track.displayName,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
            Text(
                text = "${track.duration}",
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.tagString,
                textAlign = TextAlign.Left,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
fun TrackItemPreview() {
    TrackItem(
        track = UiTrack(
            "",
            "011 Track Name",
            100,
            "Mindlessness, FOMO",
            "12:00"
        )
    )
}