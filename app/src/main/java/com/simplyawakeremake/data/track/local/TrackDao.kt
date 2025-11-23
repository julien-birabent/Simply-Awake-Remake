package com.simplyawakeremake.data.track.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks")
    fun getAll(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    fun getById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tracks: List<TrackEntity>)
}
