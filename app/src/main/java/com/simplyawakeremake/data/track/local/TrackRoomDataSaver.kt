package com.simplyawakeremake.data.track.local


import com.simplyawakeremake.data.common.DataSaver

class TrackRoomDataSaver(
    private val trackDao: TrackDao
) : DataSaver<TrackEntity> {

    override fun persist(objects: List<TrackEntity>) {
        trackDao.insertAll(objects)
    }

    override suspend fun loadAll(): List<TrackEntity> {
        return trackDao.getAll()
    }

    override fun select(id: String): TrackEntity? {
        return trackDao.getById(id)
    }
}
