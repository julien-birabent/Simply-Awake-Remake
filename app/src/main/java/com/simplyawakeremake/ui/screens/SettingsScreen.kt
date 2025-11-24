package com.simplyawakeremake.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.simplyawakeremake.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = koinViewModel()) {

    GoogleSignInSection(viewModel = viewModel)
}