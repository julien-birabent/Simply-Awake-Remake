package com.simplyawakeremake.ui.common

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ImmersiveMode(isEnabled: Boolean) {
    val view = LocalView.current

    DisposableEffect(isEnabled) {
        val activity = view.context as? Activity
        val window = activity?.window

        if (window == null) {
            return@DisposableEffect onDispose { }
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (isEnabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (isEnabled) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
