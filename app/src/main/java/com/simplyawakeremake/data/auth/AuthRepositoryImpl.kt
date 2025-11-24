package com.simplyawakeremake.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.simplyawakeremake.data.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Guest)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    init {
        val firebaseUser = firebaseAuth.currentUser
        _authState.value = if (firebaseUser == null) {
            AuthState.Guest
        } else {
            AuthState.Authenticated(
                firebaseUid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName
            )
        }

        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _authState.value = if (user == null) {
                AuthState.Guest
            } else {
                AuthState.Authenticated(
                    firebaseUid = user.uid,
                    email = user.email,
                    displayName = user.displayName
                )
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthState {
        Log.i("AuthRepositoryImpl", "signInWithGoogle: $idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        val result = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: error("Firebase user is null after sign-in")
        Log.i("AuthRepositoryImpl", "firebase user found after signin: $firebaseUser")

        val firebaseUid = firebaseUser.uid
        val email = firebaseUser.email
        val displayName = firebaseUser.displayName

        userRepository.linkCurrentUserToFirebase(
            firebaseUid = firebaseUid,
            email = email,
            displayName = displayName
        )

        val newState = AuthState.Authenticated(
            firebaseUid = firebaseUid,
            email = email,
            displayName = displayName
        )
        _authState.value = newState

        return newState
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Guest
    }
}
