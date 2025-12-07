package com.simplyawakeremake.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simplyawakeremake.data.track.local.TrackDao
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.user.UserDao
import com.simplyawakeremake.data.user.UserEntity
import com.simplyawakeremake.data.usertrack.local.UserTrackDao
import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

@Database(
    entities = [
        TrackEntity::class,
        UserTrackEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun userTrackDao(): UserTrackDao
    abstract fun userDao(): UserDao
}