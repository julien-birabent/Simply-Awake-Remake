package com.simplyawakeremake.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.R
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.data.usertrack.sync.UserTrackSyncResult
import com.simplyawakeremake.ui.UiText
import com.simplyawakeremake.usecases.GoogleSignInUseCase
import com.simplyawakeremake.usecases.download.DeleteAllDownloadsUseCase
import com.simplyawakeremake.usecases.download.ObserveActiveDownloadsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed interface SettingsEvent {
    data class LaunchGoogleSignIn(val intent: android.content.Intent) : SettingsEvent
    data class ShowMessage(val message: UiText) : SettingsEvent
}

data class SettingsUiState(
    val isLoadingUser: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val userDisplayName: String? = null,
    val errorMessage: String? = null,
    val showSyncDialog: Boolean = false,
    val isDeletingDownloads: Boolean = false,
    val lastDeleteSucceeded: Boolean? = null,
    val trackFilesCount: Int = 0,
    val trackFilesSizeBytes: Long = 0L,
    val hasActiveTrackDownloads: Boolean = false,
    val remainingTracksDownloading: Int = 0,
)

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val deleteAllDownloadsUseCase: DeleteAllDownloadsUseCase,
    private val trackFileManager: TrackFileManager,
    private val observeActiveDownloadsUseCase: ObserveActiveDownloadsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        observeCurrentUser()
        observeTrackFilesCount()
        observeActiveDownloads()
    }

    private fun observeActiveDownloads() {
        viewModelScope.launch {
            observeActiveDownloadsUseCase().collect { downloadState ->
                _uiState.update {
                    it.copy(
                        hasActiveTrackDownloads = downloadState.hasActiveDownloads,
                        remainingTracksDownloading = downloadState.remainingTracks
                    )
                }
            }
        }
    }

    private fun observeTrackFilesCount() {
        viewModelScope.launch {
            trackFileManager.trackFilesUsage.collect { fileUsage ->
                _uiState.update {
                    it.copy(
                        trackFilesCount = fileUsage.count,
                        trackFilesSizeBytes = fileUsage.totalSizeBytes
                    )
                }
            }
        }
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
        if (_uiState.value.isLoggingIn || _uiState.value.showSyncDialog) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }
            val intent = googleSignInUseCase.getSignInIntent()
            _events.emit(SettingsEvent.LaunchGoogleSignIn(intent))
        }
    }

    fun onGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoggingIn = true, errorMessage = null) }

                googleSignInUseCase.handleSignInResult(data)
                _uiState.update {
                    it.copy(isLoggingIn = false, showSyncDialog = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        errorMessage = e.message ?: "Failed to log in"
                    )
                }
                _events.emit(
                    SettingsEvent.ShowMessage(
                        UiText.StringResource(
                            R.string.settings_login_failed_toast,
                            listOf(e.message ?: "unknown error")
                        )
                    )
                )
            }
        }
    }

    fun onSyncCompleted(result: UserTrackSyncResult) {
        _uiState.update { it.copy(showSyncDialog = false) }

        viewModelScope.launch {
            when (result) {
                is UserTrackSyncResult.Success -> {
                    _events.emit(
                        SettingsEvent.ShowMessage(
                            UiText.StringResource(R.string.settings_sync_success_toast)
                        )
                    )
                }

                is UserTrackSyncResult.Failure -> {
                    _events.emit(
                        SettingsEvent.ShowMessage(
                            UiText.StringResource(
                                R.string.settings_sync_failure_toast,
                                listOf(result.cause.message ?: "unknown error")
                            )
                        )
                    )
                }
            }
        }
    }

    fun onDeleteAllDownloadsClicked() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeletingDownloads = true,
                    lastDeleteSucceeded = null
                )
            }

            val success = deleteAllDownloadsUseCase()

            _uiState.update {
                it.copy(
                    isDeletingDownloads = false,
                    lastDeleteSucceeded = success
                )
            }

            val message = if (success) {
                UiText.StringResource(R.string.settings_downloads_delete_success)
            } else {
                UiText.StringResource(R.string.settings_downloads_delete_partial_failure)
            }

            _events.emit(SettingsEvent.ShowMessage(message))
        }
    }
}