package com.simplyawakeremake.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions


fun Context.createGoogleSignInClient(): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(GoogleAuthConfig.WEB_CLIENT_ID)
        .requestEmail()
        .build()

    return GoogleSignIn.getClient(this, gso)
}


object GoogleAuthConfig {
    const val WEB_CLIENT_ID = "1028914958582-acguoha7dpvp0dfnjcuup5sacc3s87tg.apps.googleusercontent.com"
}