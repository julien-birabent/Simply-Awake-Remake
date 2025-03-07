package com.simplyawakeremake.data.track

import com.simplyawakeremake.ui.model.UiTrack
import com.simplyawakeremake.data.common.DataRepository
import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.common.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TrackRepository : DataRepository<UiTrack, ApiTrack, ApiTrack>(), TrackRepositoryInterface,
    KoinComponent {

    class TrackNotFoundException(id: String) : Exception("The track with id $id was not found")

    private val trackService: TrackService by inject()
    override val fetchAllCall: suspend () -> List<ApiTrack>
        get() = { trackService.fetchAll() }
    override val saver: DataSaver<ApiTrack> by inject(qualifier = named("tracks"))

    override val dtoToDbMapper: (ApiTrack) -> ApiTrack = { it -> it }
    override val dbToUiModelMapper: (ApiTrack) -> UiTrack = { it.toUiTrack() }

    override fun getTrackBy(id: String): Flow<ResultState<UiTrack>> = flow {
        emit(ResultState.Loading(null))
        emit(selectTrackResult(id))
    }

    override fun getAllTracks(): Flow<ResultState<List<UiTrack>>> = getAll()

    private fun selectTrackResult(id: String): ResultState<UiTrack> {
        val trackSelected = saver.select(id)
        return if (trackSelected == null) {
            ResultState.Error(TrackNotFoundException(id), null)
        } else {
            ResultState.Success(trackSelected.toUiTrack())
        }
    }
}