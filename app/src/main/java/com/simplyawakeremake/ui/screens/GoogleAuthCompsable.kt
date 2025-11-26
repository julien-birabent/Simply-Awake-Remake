package com.simplyawakeremake.ui.screens

import android.app.Activity
import android.util.Log
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.simplyawakeremake.R
import com.simplyawakeremake.data.auth.AuthState
import com.simplyawakeremake.data.auth.GoogleAuthConfig
import com.simplyawakeremake.viewmodel.SettingsViewModel

@Composable
fun GoogleSignInSection(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
        ?: error("GoogleSignInSection must run in an Activity context")

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        GoogleSignIn.getClient(activity, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleSignIn", "ActivityResult: code=${result.resultCode}")

        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleSignIn", "Got account=${account.email}, idToken null? ${idToken == null}")

            if (idToken != null) {
                viewModel.onGoogleSignInSuccessful(idToken)
            } else {
                Log.e("GoogleSignIn", "idToken is null – check requestIdToken(WEB_CLIENT_ID)")
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google sign-in failed. statusCode=${e.statusCode}", e)
            // Map statusCode to reason
            // e.g. GoogleSignInStatusCodes.SIGN_IN_CANCELLED, DEVELOPER_ERROR, etc.
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
