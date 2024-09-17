package com.example.simplyawakeremake.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.simplyawakeremake.R
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.navigation.Screen
import com.example.simplyawakeremake.viewmodel.PlayerListUIState
import com.example.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayListScreen(navController: NavController, viewModel: TrackListViewModel = koinViewModel()) {

    val uiState by viewModel.screenState.subscribeAsState(initial = PlayerListUIState.Loading)

    when (uiState) {
        is PlayerListUIState.Error -> {
            CommonErrorView(throwable = (uiState as PlayerListUIState.Error).throwable)
        }

        PlayerListUIState.Loading -> {
            LoadingIndicator()
        }

        is PlayerListUIState.Tracks -> {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Playlist(tracks = (uiState as PlayerListUIState.Tracks).items, navController)
            }
        }
    }
}

@Composable
fun Playlist(tracks: List<UiTrack>, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            count = tracks.size,
            key = { tracks[it].id },
            itemContent = { index ->
                TrackItem(tracks[index]) { id -> navController.navigate(Screen.NOW_PLAYING.name + "/${id}") }
                if (index < tracks.lastIndex)
                    HorizontalDivider(color = Color.White, thickness = 1.dp)
            }
        )
    }
}

@Composable
fun TrackItem(track: UiTrack, navigateToTrack: (id: String) -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .clickable(onClick = { navigateToTrack(track.id) })
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
    ) {}
}