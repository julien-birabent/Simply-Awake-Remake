package com.example.simplyawakeremake.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.simplyawakeremake.R
import com.example.simplyawakeremake.data.track.TrackUriProvider
import com.example.simplyawakeremake.viewmodel.NowPlayingViewModel
import com.example.simplyawakeremake.viewmodel.TrackListViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val uiModule = module {

    single { TrackUriProvider(androidApplication().getString(R.string.baseCDNUrl)) }
    single { ExoPlayer.Builder(androidApplication()).build() }
    viewModel { TrackListViewModel(androidApplication()) }
    viewModel { NowPlayingViewModel(androidApplication()) }
}