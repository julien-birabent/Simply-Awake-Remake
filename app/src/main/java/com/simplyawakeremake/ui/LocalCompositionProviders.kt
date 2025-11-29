package com.simplyawakeremake.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.simplyawakeremake.viewmodel.MainViewModel

val LocalMainViewModel = staticCompositionLocalOf<MainViewModel> {
    error("LocalMainViewModel not provided")
}