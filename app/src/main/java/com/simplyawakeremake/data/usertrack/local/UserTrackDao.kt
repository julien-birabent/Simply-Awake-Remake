package com.simplyawakeremake.data.usertrack.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTrackDao {

    @Query("SELECT * FROM user_tracks WHERE userId = :userId")
    fun observeAllForUser(userId: String): Flow<List<UserTrackEntity>>

    @Query("SELECT * FROM user_tracks WHERE userId = :userId AND trackId = :trackId LIMIT 1")
    fun observeForUserAndTrack(userId: String, trackId: String): Flow<UserTrackEntity?>

    @Query("SELECT * FROM user_tracks WHERE userId = :userId AND trackId = :trackId LIMIT 1")
    fun getForUserAndTrack(userId: String, trackId: String): UserTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userTrack: UserTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(userTracks: List<UserTrackEntity>)
}
