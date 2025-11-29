package com.simplyawakeremake.ui.screens.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.simplyawakeremake.R
import com.simplyawakeremake.navigation.Screen
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.common.LoadingButton
import com.simplyawakeremake.ui.common.LoadingIndicator
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(
    navController: NavController,
    navigateToMainRoute: String,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val mainViewModel = LocalMainViewModel.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.updateToolbar { current ->
            current.copy(showBackButton = false, showToolbar = false)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LaunchGoogleSignIn -> {
                    googleSignInLauncher.launch(event.intent)
                }

                LoginEvent.NavigateToMain -> {
                    navController.navigate(navigateToMainRoute) {
                        popUpTo(Screen.LOGIN.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    LoginScreen(
        state = uiState,
        onContinueAsGuest = viewModel::onContinueAsGuestClicked,
        onLoginWithGoogle = viewModel::onLoginWithGoogleClicked,
        onWhyLogin = viewModel::onWhyLoginClicked,
        onDismissWhyLogin = viewModel::onWhyLoginDialogDismissed,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onContinueAsGuest: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onWhyLogin: () -> Unit,
    onDismissWhyLogin: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isCheckingExistingUser -> {
                LoadingIndicator()
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.enzo),
                        contentDescription = stringResource(R.string.login_track_artwork_cd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(100.dp, 200.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LoadingButton(
                        text = stringResource(R.string.login_continue_as_guest),
                        onClick = onContinueAsGuest,
                        isLoading = false,
                        enabled = !state.isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LoadingButton(
                        text = stringResource(R.string.login_sign_in_with_google),
                        onClick = onLoginWithGoogle,
                        isLoading = state.isLoggingIn,
                        enabled = !state.isLoggingIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.login_why_login_question),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(enabled = !state.isLoggingIn) {
                            onWhyLogin()
                        },
                        textAlign = TextAlign.Center,
                    )
                }

                if (state.showWhyLoginDialog) {
                    WhyLoginDialog(onDismiss = onDismissWhyLogin)
                }
            }
        }
    }
}

@Composable
private fun WhyLoginDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.login_dialog_ok))
            }
        },
        title = {
            Text(stringResource(R.string.login_dialog_title))
        },
        text = {
            Text(stringResource(R.string.login_dialog_text))
        }
    )
}

@Preview(
    showBackground = true,
    name = "Login – Idle"
)
@Composable
fun LoginScreenIdlePreview() {
    SimplyAwakeRemakeTheme {
        LoginScreen(
            state = LoginUiState(
                isCheckingExistingUser = false,
                isLoggingIn = false,
                showWhyLoginDialog = false,
            ),
            onContinueAsGuest = {},
            onLoginWithGoogle = {},
            onWhyLogin = {},
            onDismissWhyLogin = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Login – Logging in & dialog"
)
@Composable
fun LoginScreenLoggingInPreview() {
    SimplyAwakeRemakeTheme {
        LoginScreen(
            state = LoginUiState(
                isCheckingExistingUser = false,
                isLoggingIn = true,
                showWhyLoginDialog = true,
            ),
            onContinueAsGuest = {},
            onLoginWithGoogle = {},
            onWhyLogin = {},
            onDismissWhyLogin = {},
        )
    }
}
