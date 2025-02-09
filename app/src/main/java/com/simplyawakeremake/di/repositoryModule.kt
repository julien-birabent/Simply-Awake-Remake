package com.simplyawakeremake.di

import com.simplyawakeremake.data.track.TrackRepository
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import org.koin.dsl.module

val repositoryModule = module {

    single<TrackRepositoryInterface> { TrackRepository() }
}