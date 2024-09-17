package com.example.simplyawakeremake.di

import android.content.Context
import android.content.SharedPreferences
import com.example.simplyawakeremake.data.common.DataSaver
import com.example.simplyawakeremake.data.track.ApiTrack
import com.example.simplyawakeremake.data.track.TrackSharedPrefsSaver
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module


val appModule = module {

    single<SharedPreferences> { androidContext().getSharedPreferences("private_shared_preferences_tracks", Context.MODE_PRIVATE)}
    single<DataSaver<ApiTrack>> (named("tracks")){ TrackSharedPrefsSaver(get()) }
}