package com.simplyawakeremake.ui

import androidx.annotation.StringRes
import com.simplyawakeremake.R

data class ToolbarConfig(
    @StringRes val title: Int = R.string.app_name,
    val actions: List<ToolbarAction> = emptyList(),
    val showToolbar: Boolean = false,
    val showBackButton: Boolean = false
)