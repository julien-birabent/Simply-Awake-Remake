package com.example.simplyawakeremake.data.track

import com.example.simplyawakeremake.UiTrack
import com.example.simplyawakeremake.data.common.DataRepository
import com.example.simplyawakeremake.data.common.DataSaver
import com.example.simplyawakeremake.data.common.ResultState
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Flowables
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TrackRepository: DataRepository<UiTrack, ApiTrack, ApiTrack>(), KoinComponent {

    private val trackService: TrackService by inject()
    override val fetchAllCall: () -> Single<List<ApiTrack>>
        get() = { trackService.fetchAll() }
    override val saver: DataSaver<ApiTrack> by inject(qualifier = named("tracks"))

    override val dtoToDbMapper: (ApiTrack) -> ApiTrack = { it -> it }
    override val dbToUiModelMapper: (ApiTrack) -> UiTrack = { it.toUiTrack() }

    fun getTrackBy(id: String): Flowable<ResultState<UiTrack>> =
        Flowable.concat(Flowable.just(ResultState.Loading(null)), selectTrackResult(id))

    private fun selectTrackResult(id: String): Flowable<ResultState<UiTrack>> =
        Flowables.create(mode = BackpressureStrategy.LATEST) { emitter ->
            when (val trackSelected = saver.select(id)) {
                null -> emitter.onNext(ResultState.Error(Exception("TODO"), null))
                else -> emitter.onNext(ResultState.Success(trackSelected.toUiTrack()))
            }
        }
}