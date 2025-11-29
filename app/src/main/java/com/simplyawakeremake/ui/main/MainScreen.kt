package com.simplyawakeremake.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.simplyawakeremake.navigation.AppNavHost
import com.simplyawakeremake.ui.LocalMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    canNavigateBack: Boolean = false
) {
    val mainViewModel = LocalMainViewModel.current
    val toolbarConfig by mainViewModel.toolbarConfig.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (toolbarConfig.showToolbar) {
                TopAppBar(
                    title = { Text(stringResource(id = toolbarConfig.title)) },
                    navigationIcon = {
                        if (toolbarConfig.showBackButton && canNavigateBack) {
                            run { BackButton(navController = navController) }
                        }
                    },
                    actions = {
                        toolbarConfig.actions.forEach { action ->
                            IconButton(onClick = action.onClick) {
                                Icon(action.icon, contentDescription = action.contentDescription)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        AppNavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController
        )
    }
}

@Composable
fun BackButton(navController: NavHostController) {
    IconButton(onClick = { navController.popBackStack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
