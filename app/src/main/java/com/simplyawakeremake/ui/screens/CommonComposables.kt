package com.simplyawakeremake.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = Color.White,
            strokeWidth = 5.dp
        )
    }
}

@Composable
fun CommonErrorView(throwable: Throwable) {
    Text(
        text = throwable.message ?: "An error has happened",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun <T> ItemList(
    modifier: Modifier,
    items: List<T>,
    keySelector: ((index: Int) -> Any) = {},
    onclick: (T) -> Unit,
    divider: @Composable () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            count = items.size,
            key = { keySelector(it) },
            itemContent = { index ->
                val item = items[index]
                Surface(
                    Modifier
                        .wrapContentSize()
                        .clickable { onclick(item) }) {
                    itemContent(item)
                }
                if (index < items.lastIndex) divider()
            }
        )
    }
}

@Composable
fun AskForPermissionExternalStorage(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    showRationale: () -> Unit
) {
    LaunchPermissionFlow(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        minApiLevel = Build.VERSION_CODES.Q,
        onGranted = onGranted,
        onDenied = onDenied,
        showRationale = showRationale
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LaunchPermissionFlow(
    permission: String,
    minApiLevel: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    showRationale: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= minApiLevel) {
        val permissionState = rememberPermissionState(permission)

        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }

        when {
            permissionState.status.isGranted -> onGranted()
            permissionState.status.shouldShowRationale -> showRationale()
            else -> onDenied()
        }
    } else {
        onGranted()
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}