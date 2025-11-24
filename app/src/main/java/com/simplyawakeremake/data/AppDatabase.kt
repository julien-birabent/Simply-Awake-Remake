package com.simplyawakeremake.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simplyawakeremake.data.track.local.TrackDao
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.usertrack.local.UserTrackDao
import com.simplyawakeremake.data.usertrack.local.UserTrackEntity

@Database(
    entities = [
        TrackEntity::class,
        UserTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun userTrackDao(): UserTrackDao
}