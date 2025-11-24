package com.simplyawakeremake.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.simplyawakeremake.viewmodel.MainViewModel
import com.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.simplyawakeremake.viewmodel.RecentHistoryViewModel
import com.simplyawakeremake.viewmodel.SettingsViewModel
import com.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val uiModule = module {

    single { ExoPlayer.Builder(androidApplication()).build() }
    viewModel { MainViewModel(androidApplication(), get()) }
    viewModel { TrackListViewModel(get(), get(), get(), get()) }
    viewModel { NowPlayingViewModel(androidApplication(), get()) }
    viewModel { RecentHistoryViewModel(get(), get()) }
    viewModel {
        SettingsViewModel(
            authRepository = get(),
            userTrackRepository = get(),
            userRepository = get()
        )
    }
}