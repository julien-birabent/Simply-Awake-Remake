package com.simplyawakeremake.di

import com.simplyawakeremake.usecases.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import com.simplyawakeremake.usecases.GetRecentHistoryUseCase
import org.koin.dsl.module

val domainModule = module {
    single { DownloadTrackListUseCase(get()) }
    single { CheckTrackDownloadStatusUseCase(get()) }
    single { AddTrackToRecentHistoryUseCase(get()) }
    single { GetRecentHistoryUseCase(get()) }
}