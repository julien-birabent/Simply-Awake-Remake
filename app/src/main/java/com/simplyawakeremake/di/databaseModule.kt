package com.simplyawakeremake.di

import androidx.room.Room
import com.simplyawakeremake.data.AppDatabase
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.local.TrackRoomDataSaver
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val DB_NAME = "simply-awake-db"

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            DB_NAME
        ).build()
    }

    single { get<AppDatabase>().trackDao() }

    single<DataSaver<TrackEntity>>(named("tracks")) {
        TrackRoomDataSaver(get())  // inject TrackDao
    }
}
