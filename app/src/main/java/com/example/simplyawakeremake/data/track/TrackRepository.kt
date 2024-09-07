package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.DataRepository
import com.example.simplyawakeremake.data.common.DataSaver
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TrackRepository() : DataRepository<UiTrack, ApiTrack, ApiTrack>(), KoinComponent {

    private val trackService: TrackService by inject()
    override val fetchAllCall: () -> Single<List<ApiTrack>>
        get() = { trackService.fetchAll() }
    override val saver: DataSaver<ApiTrack> by inject(qualifier = named("tracks"))

    override val dtoToDbMapper: (ApiTrack) -> ApiTrack = { it -> it }
    override val dbToUiModelMapper: (ApiTrack) -> UiTrack = { it.toUiTrack() }

}