package com.simplyawakeremake.ui.tracklist

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simplyawakeremake.ConnectionState
import com.simplyawakeremake.R
import com.simplyawakeremake.connectionState
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.common.CommonErrorView
import com.simplyawakeremake.ui.common.ConfirmationDialog
import com.simplyawakeremake.ui.common.FavoriteButton
import com.simplyawakeremake.ui.common.ItemList
import com.simplyawakeremake.ui.common.ToolbarAction
import com.simplyawakeremake.ui.common.ToolbarConfig
import com.simplyawakeremake.ui.common.goToSettingsAction
import com.simplyawakeremake.ui.common.tracksDownloadAction
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.net.UnknownHostException
import java.util.Locale
import com.simplyawakeremake.ui.common.LoadingIndicator as LoadingIndicator1

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun PlayListScreen(
    navController: NavController,
    viewModel: TrackListViewModel = koinViewModel()
) {
    val uiState by viewModel.screenState.collectAsState(initial = PlayerListUIState.Loading)
    val downloadState by viewModel.downloadState.collectAsState()
    val mainViewModel = LocalMainViewModel.current

    val connectionState by connectionState()

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Available) {
            viewModel.onConnectionAvailableForInitialSync()
        }
    }


    SetupToolbar(viewModel, mainViewModel, downloadState, navController)

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
                    navController = navController,
                    viewModel = viewModel
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
    downloadState: DownloadProgress,
    navController: NavController
) {
    var showDownloadConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    if (showDownloadConfirmationDialog) {
        DownloadConfirmationDialog(
            onConfirmSelected = {
                viewModel.downloadAllTracks()
                showDownloadConfirmationDialog = false
            },
            onDismiss = { showDownloadConfirmationDialog = false }
        )
    }

    val toolbarConfig = ToolbarConfig(
        actions = listOf(
            tracksDownloadAction {
                if (downloadState !is DownloadProgress.InProgress) {
                    showDownloadConfirmationDialog = true
                }
            },
            ToolbarAction(
                Icons.Outlined.History,
                stringResource(R.string.playlist_toolbar_recent_history)
            ) {
                navController.navigate(Screen.RECENT_HISTORY.name)
            },
            goToSettingsAction {
                navController.navigate(Screen.SETTINGS.name)
            }
        ),
        showToolbar = true
    )

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }
}

@Composable
private fun DownloadConfirmationDialog(
    onConfirmSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = stringResource(R.string.playlist_download_confirm_title),
        message = stringResource(R.string.playlist_download_confirm_message),
        confirmButtonText = stringResource(R.string.playlist_download_confirm_start),
        dismissButtonText = stringResource(R.string.playlist_download_confirm_cancel),
        onDismiss = onDismiss,
        onConfirm = { onConfirmSelected() }
    )
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
            text = stringResource(R.string.playlist_no_internet_title),
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
            text = stringResource(R.string.playlist_no_internet_message),
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
                text = stringResource(R.string.playlist_no_internet_try_again),
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
    tracks: List<Track>,
    navController: NavController,
    viewModel: TrackListViewModel
) {
    val context = LocalContext.current
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    val showToast = {
        currentToast?.cancel()
        currentToast = Toast.makeText(
            context,
            context.getString(R.string.playlist_no_internet_track_not_available),
            Toast.LENGTH_LONG
        )
        currentToast?.show()
    }
    val connectionState by connectionState()

    HorizontalDivider(color = Color.White, thickness = 1.dp)
    ItemList(
        modifier = modifier,
        items = tracks,
        keySelector = { index -> tracks[index].id },
        divider = { HorizontalDivider(color = Color.White, thickness = 1.dp) },
    ) { track ->
        TrackListItem(
            track = track,
            onClick = {
                if (viewModel.isTrackDownloaded(track.id) || connectionState == ConnectionState.Available) {
                    viewModel.addToHistory(track)
                    navController.navigate(Screen.NOW_PLAYING.name + "/${track.id}")
                } else showToast()
            },
            onFavoriteClick = { viewModel.onFavoriteClicked(it) })
    }
}

@Composable
fun TrackListItem(
    track: Track,
    onClick: (Track) -> Unit,
    onFavoriteClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(track) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = track.ordinal.toString(),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (track.tagString.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.tagString,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = track.duration,
                style = MaterialTheme.typography.bodyMedium
            )

            FavoriteButton(onClick = { onFavoriteClick(track) }, track.isFavorite)
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
            Text(
                stringResource(R.string.playlist_download_dismiss)
                    .uppercase(Locale.ROOT),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
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
                            text = stringResource(
                                R.string.playlist_download_in_progress,
                                downloadState.percentage
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onCancelClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.playlist_download_cancel),
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        text = stringResource(R.string.playlist_download_complete),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    dismissButton()
                }

                is DownloadProgress.Failure -> {
                    Text(
                        text = stringResource(
                            R.string.playlist_download_failed,
                            downloadState.error.message ?: ""
                        ),
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
