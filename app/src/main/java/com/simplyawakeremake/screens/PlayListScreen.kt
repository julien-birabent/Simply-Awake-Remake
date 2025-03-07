package com.simplyawakeremake.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simplyawakeremake.ConnectionState
import com.simplyawakeremake.R
import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.connectionState
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.ui.ToolbarAction
import com.simplyawakeremake.ui.ToolbarConfig
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.viewmodel.MainViewModel
import com.simplyawakeremake.viewmodel.PlayerListUIState
import com.simplyawakeremake.viewmodel.TrackListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.net.UnknownHostException
import java.util.Locale
import com.simplyawakeremake.screens.LoadingIndicator as LoadingIndicator1

@Composable
fun PlayListScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: TrackListViewModel = koinViewModel()
) {
    val uiState by viewModel.screenState.collectAsState(initial = PlayerListUIState.Loading)
    val downloadState by viewModel.downloadState.collectAsState()

    SetupToolbar(viewModel, mainViewModel, downloadState)

    Box(modifier = Modifier.fillMaxSize()) {
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
                Playlist(
                    modifier = Modifier.fillMaxSize(),
                    tracks = (uiState as PlayerListUIState.Tracks).items,
                    navController,
                    viewModel
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    DownloadProgressIndicator(
                        downloadState = downloadState,
                        onDismiss = viewModel::resetDownloadState,
                        onCancelClick = viewModel::cancelDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupToolbar(
    viewModel: TrackListViewModel,
    mainViewModel: MainViewModel,
    downloadState: DownloadProgress
) {
    var showDownloadConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (showDownloadConfirmationDialog) {
        DownloadConfirmationDialog(onConfirmSelected = {
            viewModel.downloadAllTracks()
            showDownloadConfirmationDialog = false
        }, onDismiss = { showDownloadConfirmationDialog = false })
    }

    val toolbarConfig = ToolbarConfig(
        actions = listOf(
            tracksDownloadAction {
                if (downloadState !is DownloadProgress.InProgress) showDownloadConfirmationDialog =
                    true
            }
        ),
        showToolbar = true
    )

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }
}

@Composable
private fun tracksDownloadAction(onClick: () -> Unit): ToolbarAction {
    return ToolbarAction(Icons.Outlined.FileDownload, contentDescription = "Download") {
        onClick()
    }
}

@Composable
private fun DownloadConfirmationDialog(
    onConfirmSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Confirm Download",
        message = "This action will download all the tracks to your device. Do you want to proceed?",
        confirmButtonText = "Start Download",
        dismissButtonText = "Cancel",
        onDismiss = onDismiss,
        onConfirm = { onConfirmSelected() })
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

@ExperimentalCoroutinesApi
@Composable
fun Playlist(
    modifier: Modifier = Modifier,
    tracks: List<UiTrack>,
    navController: NavController,
    viewModel: TrackListViewModel
) {
    val connectionState by connectionState()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            count = tracks.size,
            key = { tracks[it].id },
            itemContent = { index ->
                TrackItem(tracks[index], viewModel, connectionState) { id ->
                    navController.navigate(Screen.NOW_PLAYING.name + "/${id}")
                }
                if (index < tracks.lastIndex)
                    HorizontalDivider(color = Color.White, thickness = 1.dp)
            }
        )
    }
}

@Composable
fun TrackItem(
    track: UiTrack,
    viewModel: TrackListViewModel,
    connectionState: ConnectionState,
    navigateToTrack: (id: String) -> Unit
) {

    val context = LocalContext.current
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    val showToast = {
        currentToast?.cancel()
        currentToast = Toast.makeText(context, "No internet & track not downloaded!", Toast.LENGTH_LONG)
        currentToast?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .clickable(onClick = {
                if(viewModel.isTrackDownloaded(track.id) || connectionState == ConnectionState.Available){
                    navigateToTrack(track.id)
                } else showToast()
            })
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
                text = track.duration,
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
fun DownloadProgressIndicator(
    modifier: Modifier = Modifier,
    downloadState: DownloadProgress,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(downloadState) {
        isVisible = downloadState !is DownloadProgress.Idle
    }

    val dismissButton = @Composable {
        Button(
            onClick = {
                onDismiss()
                isVisible = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RectangleShape
        ) {
            Text("Dismiss".uppercase(Locale.ROOT), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }

    AnimatedVisibility(visible = isVisible && downloadState !is DownloadProgress.Idle) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (downloadState) {
                is DownloadProgress.InProgress -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloading... ${downloadState.percentage}%",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onCancelClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }

                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadState.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(horizontal = 16.dp),
                    )
                }

                is DownloadProgress.Success -> {
                    Text(
                        text = "Download Complete!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    dismissButton()
                }

                is DownloadProgress.Failure -> {
                    Text(
                        text = "Download Failed: ${downloadState.error.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    dismissButton()
                }

                DownloadProgress.Idle -> Unit
            }
        }
    }
}
