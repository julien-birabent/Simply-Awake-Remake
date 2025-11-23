package com.simplyawakeremake.data.track.repository

import com.simplyawakeremake.data.common.DataRepository
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.Track
import com.simplyawakeremake.data.track.local.TrackEntity
import com.simplyawakeremake.data.track.remote.TrackDto
import com.simplyawakeremake.data.track.remote.TrackService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TrackRepository : DataRepository<Track, TrackDto, TrackEntity>(),
    TrackRepositoryInterface,
    KoinComponent {

    class TrackNotFoundException(id: String) : Exception("The track with id $id was not found")

    private val trackService: TrackService by inject()

    override val fetchAllCall: suspend () -> List<TrackDto>
        get() = { trackService.fetchAll() }

    override val saver: DataSaver<TrackEntity> by inject(qualifier = named("tracks"))

    override val dtoToDbMapper: (TrackDto) -> TrackEntity = { dto ->
        dto.toDomain()
    }

    override val dbToUiModelMapper: (TrackEntity) -> Track = { entity ->
        entity.toDomain()
    }

    override fun getTrackBy(id: String): Flow<ResultState<Track>> = flow {
        emit(ResultState.Loading(null))
        emit(selectTrackResult(id))
    }

    override fun getAllTracks(): Flow<ResultState<List<Track>>> = getAll()

    private fun selectTrackResult(id: String): ResultState<Track> {
        val trackSelected = saver.select(id)
        return if (trackSelected == null) {
            ResultState.Error(TrackNotFoundException(id), null)
        } else {
            ResultState.Success(trackSelected.toDomain())
        }
    }
}
