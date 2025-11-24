package com.simplyawakeremake.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.auth.User
import com.simplyawakeremake.data.auth.AuthRepository
import com.simplyawakeremake.data.auth.AuthState
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.UserTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userTrackRepository: UserTrackRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> =
        authRepository.authState
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AuthState.Guest
            )

    /*val currentUser: StateFlow<User?> =
        userRepository.currentUser
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )*/

    fun onGoogleSignInSuccessful(idToken: String) {
        viewModelScope.launch {
            try {
                val newState = authRepository.signInWithGoogle(idToken)

                // Optionally trigger sync after login:
                // userTrackRepository.syncLocalToRemoteIfLoggedIn()

                // And/or expose some UI event for "Login success"

            } catch (e: Exception) {
                // Expose error to UI, log, etc.
            }
        }
    }

    fun onSignOutClicked() {
        viewModelScope.launch {
            authRepository.signOut()
            // UI will observe authState and react
        }
    }
}
