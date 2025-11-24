package com.simplyawakeremake.data.auth

sealed class AuthState {
    data object Guest : AuthState()
    data class Authenticated(
        val firebaseUid: String,
        val email: String?,
        val displayName: String?
    ) : AuthState()
}