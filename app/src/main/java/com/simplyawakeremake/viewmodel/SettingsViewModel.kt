package com.simplyawakeremake.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.usecases.GoogleSignInUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class SettingsUiState(
    val isLoadingUser: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val userDisplayName: String? = null,
    val errorMessage: String? = null,
)

sealed interface SettingsEvent {
    data class LaunchGoogleSignIn(val intent: android.content.Intent) : SettingsEvent
    data class ShowMessage(val message: String) : SettingsEvent
}

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val googleSignInUseCase: GoogleSignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isLoadingUser = false,
                        isLoggedIn = user.firebaseUid != null,
                        userDisplayName = user.displayName ?: user.email ?: "Logged in"
                    )
                }
            }
        }
    }

    fun onLoginWithGoogleClicked() {
        if (_uiState.value.isLoggingIn) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
            val intent = googleSignInUseCase.getSignInIntent()
            _events.emit(SettingsEvent.LaunchGoogleSignIn(intent))
        }
    }

    fun onGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                googleSignInUseCase.handleSignInResult(data)
                _uiState.update { it.copy(isLoggingIn = false) }
                _events.emit(
                    SettingsEvent.ShowMessage("Successfully linked your account ✨")
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = e.message ?: "Failed to log in"
                    )
                }
                _events.emit(
                    SettingsEvent.ShowMessage("Login failed: ${e.message ?: "unknown error"}")
                )
            }
        }
    }
}
