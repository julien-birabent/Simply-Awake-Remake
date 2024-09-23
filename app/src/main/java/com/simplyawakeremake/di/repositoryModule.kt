package com.simplyawakeremake.di

import com.simplyawakeremake.data.track.TrackRepository
import org.koin.dsl.module

val repositoryModule = module {

    single { TrackRepository() }
}