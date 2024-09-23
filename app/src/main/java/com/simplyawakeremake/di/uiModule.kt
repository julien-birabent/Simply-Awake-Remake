package com.simplyawakeremake.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.simplyawakeremake.BuildConfig
import com.simplyawakeremake.data.track.TrackUriProvider
import com.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val uiModule = module {

    single { TrackUriProvider(BuildConfig.baseServerUrl) }
    single { ExoPlayer.Builder(androidApplication()).build() }
    viewModel { TrackListViewModel(androidApplication()) }
    viewModel { NowPlayingViewModel(androidApplication()) }
}