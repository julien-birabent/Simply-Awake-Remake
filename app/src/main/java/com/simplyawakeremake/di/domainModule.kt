package com.simplyawakeremake.di

import com.simplyawakeremake.data.usertrack.sync.InitialUserTrackSyncManager
import com.simplyawakeremake.usecases.history.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.download.DeleteAllDownloadsUseCase
import com.simplyawakeremake.usecases.download.DownloadTrackListUseCase
import com.simplyawakeremake.usecases.history.GetRecentHistoryUseCase
import com.simplyawakeremake.usecases.GoogleSignInUseCase
import com.simplyawakeremake.usecases.RegisterTrackPlayUseCase
import com.simplyawakeremake.usecases.download.SingleTrackDownloadUseCase
import com.simplyawakeremake.usecases.sync.SyncUserTracksAfterLoginUseCase
import com.simplyawakeremake.usecases.ToggleTrackFavoriteUseCase
import com.simplyawakeremake.usecases.download.ObserveActiveDownloadsUseCase
import com.simplyawakeremake.usecases.download.ObserveTrackDownloadsUseCase
import com.simplyawakeremake.usecases.sync.UserTrackLoginSyncUseCase
import org.koin.dsl.module

val domainModule = module {
    single { DownloadTrackListUseCase(get()) }
    single { CheckTrackDownloadStatusUseCase(get()) }
    single { AddTrackToRecentHistoryUseCase(get()) }
    single { GetRecentHistoryUseCase(get()) }
    single {
        GoogleSignInUseCase(
            googleSignInClient = get(),
            firebaseAuth = get(),
            userRepository = get()
        )
    }
    single { ToggleTrackFavoriteUseCase(userTrackRepository = get()) }
    single { RegisterTrackPlayUseCase(userTrackRepository = get()) }
    single { SyncUserTracksAfterLoginUseCase(get(), get(), get(), get()) }
    single { UserTrackLoginSyncUseCase(get(), get(), get()) }
    single { InitialUserTrackSyncManager(userRepository = get(), loginSyncUseCase = get()) }
    single { DeleteAllDownloadsUseCase(trackFileManager = get()) }
    single { SingleTrackDownloadUseCase(trackFileManager = get()) }
    factory { ObserveTrackDownloadsUseCase(trackFileManager = get()) }
    single { ObserveActiveDownloadsUseCase(trackFileManager = get()) }
}