package com.simplyawakeremake.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.simplyawakeremake.ui.main.MainViewModel

val LocalMainViewModel = staticCompositionLocalOf<MainViewModel> {
    error("LocalMainViewModel not provided")
}