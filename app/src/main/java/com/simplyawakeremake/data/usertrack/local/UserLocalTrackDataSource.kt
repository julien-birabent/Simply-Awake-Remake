package com.simplyawakeremake.data.usertrack.local

import com.simplyawakeremake.data.usertrack.UserTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserTrackLocalDataSource(
    private val dao: UserTrackDao
) {

    fun observeAll(userId: String): Flow<List<UserTrack>> =
        dao.observeAllForUser(userId).map { it.map { it.toDomain() } }

    fun observeOne(userId: String, trackId: String): Flow<UserTrack?> =
        dao.observeForUserAndTrack(userId, trackId).map { it?.toDomain() }

    suspend fun getAllForUser(userId: String): List<UserTrack> {
        return dao.getAllForUser(userId).map { it.toDomain() }
    }

    suspend fun getOne(userId: String, trackId: String): UserTrack? =
        dao.getForUserAndTrack(userId, trackId)?.toDomain()

    suspend fun upsert(entity: UserTrack) {
        dao.upsert(entity.toEntity())
    }

    suspend fun upsertAll(entities: List<UserTrack>) {
        dao.upsertAll(entities.map { it.toEntity() })
    }
}