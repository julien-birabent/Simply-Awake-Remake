package com.example.simplyawakeremake.di

import com.example.simplyawakeremake.data.track.TrackRepository
import org.koin.dsl.module

val repositoryModule = module {

    single { TrackRepository(get(), get()) }
}