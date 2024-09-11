package com.example.simplyawakeremake.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
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
            LoadingIndicator()
        }

        is PlayerListUIState.Tracks -> {
            Playlist(tracks = (uiState as PlayerListUIState.Tracks).items)
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(100.dp),
            color = Color.White,
            strokeWidth = 5.dp)
    }
}

@Composable
fun Playlist(tracks: List<UiTrack>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = tracks.size,
            key = { tracks[it].id },
            itemContent = { index ->
                TrackItem(tracks[index])
            }
        )
    }
}

@Composable
fun TrackItem(track: UiTrack) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
                .clickable(onClick = { })
                .background(Color.White)
                .padding(top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration ${track.duration}",
                    textAlign = TextAlign.Left,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
            "011 Track Name",
            120,
            "Tags : mindfullness",
            duration = "1:00",
            season = 1,
            year = 2000
        )
    )
}