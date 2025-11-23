package com.simplyawakeremake.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.local.TrackDao

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
