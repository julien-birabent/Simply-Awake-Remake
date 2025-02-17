package com.simplyawakeremake.di

import com.simplyawakeremake.domain.AddTrackToRecentHistoryUseCase
import com.simplyawakeremake.domain.GetRecentHistoryUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { AddTrackToRecentHistoryUseCase(get()) }
    single { GetRecentHistoryUseCase(get()) }
}