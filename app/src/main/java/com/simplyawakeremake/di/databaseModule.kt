package com.simplyawakeremake.di

import androidx.room.Room
import com.simplyawakeremake.data.AppDatabase
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.track.local.TrackDao
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.local.TrackRoomDataSaver
import com.simplyawakeremake.data.user.UserDao
import com.simplyawakeremake.data.usertrack.local.UserTrackDao
import com.simplyawakeremake.data.usertrack.local.UserTrackLocalDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val DB_NAME = "simply-awake-db"

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(), AppDatabase::class.java, DB_NAME
        ).build()
    }

    single<TrackDao> { get<AppDatabase>().trackDao() }
    single<UserTrackDao> { get<AppDatabase>().userTrackDao() }
    single<UserDao> { get<AppDatabase>().userDao() }

    single<DataSaver<TrackEntity>>(named("tracks")) {
        TrackRoomDataSaver(get())
    }

    single { UserTrackLocalDataSource(get()) }
}