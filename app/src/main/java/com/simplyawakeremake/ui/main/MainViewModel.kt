package com.simplyawakeremake.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.simplyawakeremake.ui.common.ToolbarConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val defaultToolbarConfig = ToolbarConfig(actions = emptyList(), showToolbar = true)

    private val _toolbarConfig = MutableStateFlow(defaultToolbarConfig)
    val toolbarConfig: StateFlow<ToolbarConfig> = _toolbarConfig

    fun updateToolbar(config: ToolbarConfig?) {
        _toolbarConfig.value = config?.copy() ?: _toolbarConfig.value.copy()
    }

    fun updateToolbar(
        builder: (current: ToolbarConfig) -> ToolbarConfig
    ) {
        _toolbarConfig.value = builder(_toolbarConfig.value)
    }
}
