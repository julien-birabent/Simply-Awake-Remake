package com.example.simplyawakeremake.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.viewmodel.PlayerListUIState
import com.example.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayListScreen(navController: NavController, viewModel: TrackListViewModel = koinViewModel()) {

    val uiState by viewModel.screenState.subscribeAsState(initial = PlayerListUIState.Loading)

    when (uiState) {
        PlayerListUIState.Error -> {

        }

        PlayerListUIState.Loading -> {

        }

        is PlayerListUIState.Tracks -> {
            Playlist(tracks = (uiState as PlayerListUIState.Tracks).items)
        }
    }
}

@Composable
fun Playlist(tracks: List<UiTrack>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(tracks) { track ->
            TrackItem(track)
        }
    }
}

@Composable
fun TrackItem(track: UiTrack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 25.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            track.name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
fun TrackItemPreview() {
    TrackItem(
        track = UiTrack(
            "",
            1,
            1,
            "Track Name",
            120,
            "Tags : mindfullness",
            duration = "1:00",
            season = 1,
            year = 2000
        )
    )
}