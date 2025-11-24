package com.simplyawakeremake.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signInWithGoogle(idToken: String): AuthState
    suspend fun signOut()
}