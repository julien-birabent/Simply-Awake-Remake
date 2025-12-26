package com.simplyawakeremake.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.simplyawakeremake.data.download.track.TrackFileManager
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import com.simplyawakeremake.data.usertrack.sync.InitialUserTrackSyncManager
import com.simplyawakeremake.ui.history.RecentHistoryViewModel
import com.simplyawakeremake.ui.login.LoginViewModel
import com.simplyawakeremake.ui.main.MainViewModel
import com.simplyawakeremake.ui.playback.NowPlayingViewModel
import com.simplyawakeremake.ui.settings.SettingsViewModel
import com.simplyawakeremake.ui.synchronization.SynchronizationViewModel
import com.simplyawakeremake.ui.trackfilter.TrackFilterViewModel
import com.simplyawakeremake.ui.tracklist.TrackListViewModel
import com.simplyawakeremake.usecases.ApplyTrackFiltersUseCase
import com.simplyawakeremake.usecases.ToggleTrackFavoriteUseCase
import com.simplyawakeremake.usecases.download.DownloadTrackListUseCase
import com.simplyawakeremake.usecases.download.ObserveTrackDownloadsUseCase
import com.simplyawakeremake.usecases.download.SingleTrackDownloadUseCase
import com.simplyawakeremake.usecases.history.AddTrackToRecentHistoryUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val uiModule = module {

    single { ExoPlayer.Builder(androidApplication()).build() }
    viewModel { MainViewModel(androidApplication()) }
    viewModel { LoginViewModel(userRepository = get(), googleSignInUseCase = get()) }
    viewModel {
        TrackListViewModel(
            trackRepository = get<TrackRepositoryInterface>(),
            downloadTrackListUseCase = get<DownloadTrackListUseCase>(),
            addTrackToRecentHistoryUseCase = get<AddTrackToRecentHistoryUseCase>(),
            toggleTrackFavoriteUseCase = get<ToggleTrackFavoriteUseCase>(),
            initialUserTrackSyncManager = get<InitialUserTrackSyncManager>(),
            trackFileManager = get<TrackFileManager>(),
            observeTrackDownloadsUseCase = get<ObserveTrackDownloadsUseCase>(),
            singleTrackDownloadUseCase = get<SingleTrackDownloadUseCase>(),
            applyTrackFiltersUseCase = get<ApplyTrackFiltersUseCase>()
        )
    }
    viewModel { NowPlayingViewModel(androidApplication(), get(), get(), get(), get()) }
    viewModel { RecentHistoryViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SynchronizationViewModel(syncUserTracksAfterLoginUseCase = get()) }
    viewModel { TrackFilterViewModel() }
}