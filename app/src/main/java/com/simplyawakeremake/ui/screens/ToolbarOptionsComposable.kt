package com.simplyawakeremake.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import com.simplyawakeremake.ui.ToolbarAction

@Composable
fun goToSettingsAction(onClick: () -> Unit): ToolbarAction {
    return ToolbarAction(Icons.Outlined.Settings, contentDescription = "Settings") {
        onClick()
    }
}

@Composable
fun tracksDownloadAction(onClick: () -> Unit): ToolbarAction {
    return ToolbarAction(Icons.Outlined.FileDownload, contentDescription = "Download") {
        onClick()
    }
}