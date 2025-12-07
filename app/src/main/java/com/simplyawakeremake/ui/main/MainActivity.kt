package com.simplyawakeremake.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.simplyawakeremake.ui.LocalMainViewModel
import com.simplyawakeremake.ui.theme.SimplyAwakeRemakeTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent

class MainActivity : ComponentActivity(), KoinComponent {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            var canNavBack by remember { mutableStateOf(false) }
            val mainViewModel = koinViewModel<MainViewModel>()

            DisposableEffect(navController) {
                val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
                    canNavBack = controller.previousBackStackEntry != null
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose {
                    navController.removeOnDestinationChangedListener(listener)
                }
            }
            SimplyAwakeRemakeTheme {
                CompositionLocalProvider(LocalMainViewModel provides mainViewModel) {
                    SetStatusBarColor(MaterialTheme.colorScheme.background)
                    MainScreen(navController = navController, canNavBack)
                }
            }
        }
    }

    @Composable
    fun SetStatusBarColor(color: Color) {
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.setSystemBarsColor(color)
        }
    }
}
