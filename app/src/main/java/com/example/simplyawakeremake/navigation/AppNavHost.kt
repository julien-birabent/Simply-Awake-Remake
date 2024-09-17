package com.example.simplyawakeremake.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.simplyawakeremake.screens.NowPlayingScreen
import com.example.simplyawakeremake.screens.PlayListScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Screen.PLAYLIST.name
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.PLAYLIST.name) {
            PlayListScreen(navController)
        }
        composable(
            Screen.NOW_PLAYING.name + "/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.StringType })
        ) { backStackEntry ->
            NowPlayingScreen(navController, backStackEntry.arguments?.getString("trackId") ?: "")
        }
    }
}