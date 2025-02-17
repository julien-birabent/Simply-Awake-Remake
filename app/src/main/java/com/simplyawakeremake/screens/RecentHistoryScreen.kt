package com.simplyawakeremake.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.viewmodel.RecentHistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentHistoryScreen(
    navController: NavController,
    viewModel: RecentHistoryViewModel = koinViewModel()) {
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
            LazyColumn {
                items(items) { historyItem ->
                    HistoryItemRow(historyItem)
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: UiTrackHistory) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = item.track.displayName, modifier = Modifier.weight(1f))
        Text(text = formatTime(item.playedTimestamp), style = MaterialTheme.typography.bodySmall)
    }
}

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
