package com.simplyawakeremake.data.usertrack.local

import kotlinx.coroutines.flow.Flow

class UserTrackLocalDataSource(
    private val dao: UserTrackDao
) {

    fun observeAll(userId: String): Flow<List<UserTrackEntity>> =
        dao.observeAllForUser(userId)

    fun observeOne(userId: String, trackId: String): Flow<UserTrackEntity?> =
        dao.observeForUserAndTrack(userId, trackId)

    suspend fun getOne(userId: String, trackId: String): UserTrackEntity? =
        dao.getForUserAndTrack(userId, trackId)

    suspend fun upsert(entity: UserTrackEntity) {
        dao.upsert(entity)
    }

    suspend fun upsertAll(entities: List<UserTrackEntity>) {
        dao.upsertAll(entities)
    }
}