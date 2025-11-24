package com.simplyawakeremake.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplyawakeremake.data.user.UserRepository
import com.simplyawakeremake.ui.ToolbarConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    userRepository: UserRepository
) : AndroidViewModel(application) {

    private val defaultToolbarConfig = ToolbarConfig(actions = emptyList(), showToolbar = true)

    private val _toolbarConfig = MutableStateFlow(defaultToolbarConfig)
    val toolbarConfig: StateFlow<ToolbarConfig> = _toolbarConfig

    fun updateToolbar(config: ToolbarConfig?) {
        _toolbarConfig.value = config?.copy() ?: _toolbarConfig.value.copy()
    }

    init {
        viewModelScope.launch {
            userRepository.ensureGuestUser()
        }
    }
}
