package com.simplyawakeremake.ui.login


import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.usecases.GoogleSignInUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isCheckingExistingUser: Boolean = false,
    val isLoggingIn: Boolean = false,
    val errorMessage: String? = null,
    val showWhyLoginDialog: Boolean = false,
)

sealed interface LoginEvent {
    data object NavigateToMain : LoginEvent
    data object StartSyncAfterLogin : LoginEvent
    data class LaunchGoogleSignIn(val intent: Intent) : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val googleSignInUseCase: GoogleSignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(isCheckingExistingUser = true))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val hasUser = userRepository.hasExistingUser()
            if (hasUser) {
                _events.emit(LoginEvent.NavigateToMain)
            } else {
                _uiState.update { it.copy(isCheckingExistingUser = false) }
            }
        }
    }

    fun onContinueAsGuestClicked() {
        if (_uiState.value.isLoggingIn || _uiState.value.isCheckingExistingUser) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
                userRepository.ensureLocalUserExists()
                _events.emit(LoginEvent.NavigateToMain)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = e.message ?: "Unexpected error"
                    )
                }
            }
        }
    }

    fun onLoginWithGoogleClicked() {
        if (_uiState.value.isLoggingIn || _uiState.value.isCheckingExistingUser) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
            val intent = googleSignInUseCase.getSignInIntent()
            _events.emit(LoginEvent.LaunchGoogleSignIn(intent))
        }
    }

    fun onGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                googleSignInUseCase.handleSignInResult(data)
                _events.emit(LoginEvent.StartSyncAfterLogin)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = e.message ?: "Login failed"
                    )
                }
            }
        }
    }

    fun onSyncAfterLoginCompleted(result: UserTrackSyncResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = false) }

            if (result is UserTrackSyncResult.Failure) {
                _events.emit(
                    LoginEvent.ShowMessage(
                        "We couldn't synchronise your data. " +
                                "You can still use the app; sync will be retried later."
                    )
                )
            }
            _events.emit(LoginEvent.NavigateToMain)
        }
    }


    fun onWhyLoginClicked() {
        _uiState.update { it.copy(showWhyLoginDialog = true) }
    }

    fun onWhyLoginDialogDismissed() {
        _uiState.update { it.copy(showWhyLoginDialog = false) }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
