package com.simplyawakeremake.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.simplyawakeremake.data.auth.AuthState
import com.simplyawakeremake.data.auth.createGoogleSignInClient
import com.simplyawakeremake.viewmodel.SettingsViewModel

@Composable
fun GoogleSignInSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    requireNotNull(activity) { "GoogleSignInSection must run in an Activity context" }

    val googleSignInClient = remember { context.createGoogleSignInClient() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // User canceled or something went wrong
            return@rememberLauncherForActivityResult
        }

        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.onGoogleSignInSuccessful(idToken)
            } else {
                // Handle null token (likely requestIdToken misconfigured)
            }
        } catch (e: ApiException) {
            // Handle Google sign-in error (expose to UI, log, etc.)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        val authState by viewModel.authState.collectAsState()
        when (authState) {
            is AuthState.Guest -> Text("You are using the app as Guest")
            is AuthState.Authenticated -> {
                val auth = authState as AuthState.Authenticated
                Text("Signed in as ${auth.displayName ?: auth.email ?: "Unknown"}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(),
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        ) {
            Text("Sign in with Google")
        }
    }
}
