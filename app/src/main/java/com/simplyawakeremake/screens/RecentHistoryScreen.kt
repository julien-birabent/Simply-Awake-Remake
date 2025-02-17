package com.simplyawakeremake.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.viewmodel.RecentHistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentHistoryScreen(
    navController: NavController,
    viewModel: RecentHistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is RecentHistoryViewModel.RecentHistoryUIState.Error -> {
            CommonErrorView(throwable = (uiState as RecentHistoryViewModel.RecentHistoryUIState.Error).throwable)
        }

        RecentHistoryViewModel.RecentHistoryUIState.Loading -> {
            LoadingIndicator()
        }

        is RecentHistoryViewModel.RecentHistoryUIState.RecentHistoryLoaded -> {
            val items =
                (uiState as RecentHistoryViewModel.RecentHistoryUIState.RecentHistoryLoaded).items
            Column(
                Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Recently played meditations",
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                ItemList(
                    items,
                    { index -> items[index].playedTimestamp },
                    { navController.navigate(Screen.NOW_PLAYING.name + "/${it.track.id}") },
                    divider = { HorizontalDivider(color = Color.White, thickness = 1.dp) },
                ) { uiTrackHistory -> HistoryItemRow(uiTrackHistory) }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: UiTrackHistory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = item.track.displayName,
            modifier = Modifier.weight(0.75f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(text = formatTime(item.playedTimestamp), style = MaterialTheme.typography.bodyMedium)
    }
}

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
