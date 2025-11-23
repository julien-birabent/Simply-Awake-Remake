package com.simplyawakeremake.data.track.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks")
    fun getAll(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    fun getById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tracks: List<TrackEntity>)
}
