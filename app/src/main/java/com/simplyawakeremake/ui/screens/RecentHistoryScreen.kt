package com.simplyawakeremake.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.ConnectionState
import com.simplyawakeremake.connectionState
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.ui.ToolbarConfig
import com.simplyawakeremake.viewmodel.MainViewModel
import com.simplyawakeremake.viewmodel.RecentHistoryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun RecentHistoryScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: RecentHistoryViewModel = koinViewModel()
) {
    val toolbarConfig = ToolbarConfig(
        actions = listOf(),
        showToolbar = true,
        showBackButton = true
    )
    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    val showToast = {
        currentToast?.cancel()
        currentToast =
            Toast.makeText(context, "No internet & track not downloaded!", Toast.LENGTH_LONG)
        currentToast?.show()
    }
    val connectionState by connectionState()

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                ItemList(
                    modifier = Modifier,
                    items,
                    { index -> items[index].playedTimestamp },
                    {
                        if (viewModel.isTrackDownloaded(it.track.id) || connectionState == ConnectionState.Available) {
                            navController.navigate(Screen.NOW_PLAYING.name + "/${it.track.id}")
                        } else showToast()
                    },
                    divider = { HorizontalDivider(color = Color.White, thickness = 1.dp) },
                ) { uiTrackHistory -> HistoryItemRow(uiTrackHistory) }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: UiTrackHistory) {
    val track = item.track
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
                text = formatTime(item.playedTimestamp),
                style = MaterialTheme.typography.bodyMedium
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

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
