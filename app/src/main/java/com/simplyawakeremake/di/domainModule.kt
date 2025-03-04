package com.simplyawakeremake.di

import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import org.koin.dsl.module

val domainModule = module {
    single { DownloadTrackListUseCase(get()) }
}