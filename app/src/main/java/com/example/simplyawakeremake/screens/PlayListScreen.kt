package com.example.simplyawakeremake.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.simplyawakeremake.R
import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.extensions.isOnline
import com.example.simplyawakeremake.navigation.Screen
import com.example.simplyawakeremake.viewmodel.PlayerListUIState
import com.example.simplyawakeremake.viewmodel.TrackListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.net.UnknownHostException
import com.example.simplyawakeremake.screens.LoadingIndicator as LoadingIndicator1

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
                Playlist(
                    tracks = (uiState as PlayerListUIState.Tracks).items,
                    navController,
                    viewModel.app
                )
            }
        }
    }
}

@Composable
private fun NoInternetScreen(tryAgainAction: () -> Unit) {

    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.baseline_wifi_off_24),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),

            )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Whoops!!",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No Internet connection was found. Check your connection or try again.",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp, start = 25.dp, end = 25.dp)
                .fillMaxWidth(),
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp),
            onClick = { scope.launch { tryAgainAction() } },
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text(
                text = "Try again",
                fontSize = 20.sp,
                color = Color.White
            )
        }

    }
}

@Composable
fun QuickDismissAlertDialog(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun Playlist(tracks: List<UiTrack>, navController: NavController, context: Context) {
    var showNoInternetDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            count = tracks.size,
            key = { tracks[it].id },
            itemContent = { index ->
                TrackItem(tracks[index]) { id ->
                    showNoInternetDialog = !context.isOnline()
                    if (!showNoInternetDialog) navController.navigate(Screen.NOW_PLAYING.name + "/${id}")
                }
                if (index < tracks.lastIndex)
                    HorizontalDivider(color = Color.White, thickness = 1.dp)
            }
        )
    }
    if (showNoInternetDialog) {
        QuickDismissAlertDialog(
            onDismissRequest = { showNoInternetDialog = false },
            dialogTitle = "Whoops",
            dialogText = "The content of the meditation cannot be loaded because you're device seems to be offline."
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