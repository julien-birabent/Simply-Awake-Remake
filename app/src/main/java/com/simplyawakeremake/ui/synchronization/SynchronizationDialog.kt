package com.simplyawakeremake.ui.synchronization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simplyawakeremake.R
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import org.koin.androidx.compose.koinViewModel


@Composable
fun SynchronizationFlowDialog(
    onFinished: (UserTrackSyncResult) -> Unit,
    viewModel: SynchronizationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startSync()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SyncEvent.Completed -> {
                    onFinished(event.result)
                }
            }
        }
    }

    if (!uiState.isVisible) return

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true,
        )
    ) {
        SynchronizationDialogContent(state = uiState)
    }
}


@Composable
fun SynchronizationDialogContent(
    state: SyncUiState,
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {},
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.sync_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = Color.White,
                        strokeWidth = 5.dp
                    )
                }

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Preview(
    showBackground = true,
    name = "Sync dialog – Running"
)
@Composable
fun SynchronizationDialogRunningPreview() {
    SimplyAwakeRemakeTheme {
        SynchronizationDialogContent(
            state = SyncUiState(
                isRunning = true,
                message = "Syncing your data…",
                errorMessage = null,
            )
        )
    }
}

@Preview(
    showBackground = true,
    name = "Sync dialog – Error"
)
@Composable
fun SynchronizationDialogErrorPreview() {
    SimplyAwakeRemakeTheme {
        SynchronizationDialogContent(
            state = SyncUiState(
                isRunning = false,
                message = "We tried to sync your data.",
                errorMessage = "Something went wrong. Please try again later.",
            )
        )
    }
}
