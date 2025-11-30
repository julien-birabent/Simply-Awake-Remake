package com.simplyawakeremake.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.R
import com.simplyawakeremake.extensions.formatSize
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.common.LoadingButton
import com.simplyawakeremake.ui.common.ToolbarConfig
import com.simplyawakeremake.ui.synchronization.SynchronizationFlowDialog
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val mainViewModel = LocalMainViewModel.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val toolbarConfig = ToolbarConfig(
        title = R.string.toolbar_title_settings,
        showToolbar = true,
        showBackButton = true
    )

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar(toolbarConfig)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.LaunchGoogleSignIn -> {
                    googleSignInLauncher.launch(event.intent)
                }

                is SettingsEvent.ShowMessage -> {
                    Toast.makeText(
                        context,
                        event.message.asString(context),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onLoginWithGoogle = viewModel::onLoginWithGoogleClicked,
        onDeleteAllDownloadsClicked = viewModel::onDeleteAllDownloadsClicked
    )

    if (uiState.showSyncDialog) {
        SynchronizationFlowDialog(
            onFinished = { result ->
                viewModel.onSyncCompleted(result)
            }
        )
    }
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onLoginWithGoogle: () -> Unit = {},
    onDeleteAllDownloadsClicked: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        HorizontalDivider(modifier = Modifier.height(1.dp))
        GoogleSignInSection(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            state = uiState,
            onLoginWithGoogle = onLoginWithGoogle
        )
        HorizontalDivider(modifier = Modifier.height(1.dp))

        DeleteDownloadsSection(
            isDeleting = uiState.isDeletingDownloads,
            trackFilesCount = uiState.trackFilesCount,
            trackFilesSizeBytes = uiState.trackFilesSizeBytes,
            onDeleteClicked = onDeleteAllDownloadsClicked
        )
        HorizontalDivider(modifier = Modifier.height(1.dp))
    }
}

@Composable
fun DeleteDownloadsSection(
    isDeleting: Boolean,
    trackFilesCount: Int,
    trackFilesSizeBytes: Long,
    onDeleteClicked: () -> Unit
) {
    val tracksLabel = pluralStringResource(
        id = R.plurals.settings_downloads_tracks,
        count = trackFilesCount,
        trackFilesCount
    )

    val formattedSize = trackFilesSizeBytes.formatSize()

    val statusText = stringResource(
        id = R.string.settings_downloads_status,
        tracksLabel,
        formattedSize
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_section_downloads_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.settings_downloads_delete_all_description),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        LoadingButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.settings_downloads_delete_all_button),
            isLoading = isDeleting,
            enabled = !isDeleting && trackFilesCount > 0,
            onClick = onDeleteClicked
        )
    }
}


@Composable
fun GoogleSignInSection(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onLoginWithGoogle: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.settings_section_account_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoadingUser -> {
                Text(
                    text = stringResource(id = R.string.settings_loading_account),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.isLoggedIn -> {
                Text(
                    text = stringResource(id = R.string.settings_logged_in_as),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.userDisplayName
                        ?: stringResource(id = R.string.settings_unknown_user),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                Text(
                    text = stringResource(id = R.string.settings_guest_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LoadingButton(
                    text = stringResource(id = R.string.settings_sign_in_with_google),
                    onClick = onLoginWithGoogle,
                    isLoading = state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    name = "Settings – Guest user"
)
@Composable
fun SettingsGuestPreview() {
    val ui = SettingsUiState(
        isLoadingUser = false,
        isLoggedIn = false,
        isLoggingIn = false,
        userDisplayName = null,
        errorMessage = null
    )
    SimplyAwakeRemakeTheme {
        SettingsScreen(uiState = ui) { }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF000000,
    name = "Settings – Logged in user"
)
@Composable
fun SettingsLoggedInPreview() {
    val ui = SettingsUiState(
        isLoadingUser = false,
        isLoggedIn = true,
        isLoggingIn = false,
        userDisplayName = "Julien",
        errorMessage = null
    )
    SimplyAwakeRemakeTheme {
        SettingsScreen(uiState = ui) { }
    }
}

