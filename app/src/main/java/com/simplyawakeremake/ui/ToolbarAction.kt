package com.simplyawakeremake.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolbarAction(
    val icon: ImageVector,
    val contentDescription: String?,
    val onClick: () -> Unit
)