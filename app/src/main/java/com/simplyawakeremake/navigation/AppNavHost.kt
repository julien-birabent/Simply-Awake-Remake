package com.simplyawakeremake.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.simplyawakeremake.ui.history.RecentHistoryScreen
import com.simplyawakeremake.ui.login.LoginRoute
import com.simplyawakeremake.ui.playback.NowPlayingScreen
import com.simplyawakeremake.ui.settings.SettingsRoute
import com.simplyawakeremake.ui.tracklist.PlayListScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Screen.LOGIN.name
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
            NowPlayingScreen(
                navController,
                backStackEntry.arguments?.getString("trackId") ?: ""
            )
        }
        composable(Screen.RECENT_HISTORY.name) { RecentHistoryScreen(navController) }
        composable(Screen.SETTINGS.name) { SettingsRoute(navController) }
        composable(Screen.LOGIN.name) {
            LoginRoute(
                navController,
                navigateToMainRoute = Screen.PLAYLIST.name
            )
        }

    }
}