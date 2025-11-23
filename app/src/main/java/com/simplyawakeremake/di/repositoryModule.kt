package com.simplyawakeremake.di

import com.simplyawakeremake.data.history.TrackHistoryRepository
import com.simplyawakeremake.data.history.TrackHistoryRepositoryInterface
import com.simplyawakeremake.data.track.repository.TrackRepository
import com.simplyawakeremake.data.track.repository.TrackRepositoryInterface
import org.koin.dsl.module

val repositoryModule = module {

    single<TrackRepositoryInterface> { TrackRepository() }
    single<TrackHistoryRepositoryInterface> { TrackHistoryRepository() }
}