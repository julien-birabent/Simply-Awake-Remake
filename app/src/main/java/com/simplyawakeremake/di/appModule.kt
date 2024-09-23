package com.simplyawakeremake.di

import android.content.Context
import android.content.SharedPreferences
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.track.ApiTrack
import com.simplyawakeremake.data.track.TrackSharedPrefsSaver
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module


val appModule = module {

    single<SharedPreferences> { androidContext().getSharedPreferences("private_shared_preferences_tracks", Context.MODE_PRIVATE)}
    single<DataSaver<ApiTrack>> (named("tracks")){ TrackSharedPrefsSaver(get()) }
}