package com.simplyawakeremake.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.simplyawakeremake.ui.screens.NowPlayingScreen
import com.simplyawakeremake.ui.screens.PlayListScreen
import com.simplyawakeremake.ui.screens.RecentHistoryScreen
import com.simplyawakeremake.viewmodel.MainViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Screen.PLAYLIST.name,
    mainViewModel: MainViewModel,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.PLAYLIST.name) {
            PlayListScreen(navController, mainViewModel)
        }
        composable(
            Screen.NOW_PLAYING.name + "/{trackId}",
            arguments = listOf(navArgument("trackId") { type = NavType.StringType })
        ) { backStackEntry ->
            NowPlayingScreen(
                navController,
                mainViewModel,
                backStackEntry.arguments?.getString("trackId") ?: ""
            )
        }
        composable(Screen.RECENT_HISTORY.name) { RecentHistoryScreen(navController, mainViewModel) }
    }
}