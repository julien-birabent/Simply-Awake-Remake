package com.example.simplyawakeremake.data.common

import io.reactivex.rxjava3.core.Flowable

abstract class DataRepository<UiModel, DTO, DB>(
    private val service: ApiService<DTO>,
    private val saver: DataSaver<DB>
) {

    abstract val dtoToDbMapper: (DTO) -> DB
    abstract val dbToUiModelMapper: (DB) -> UiModel

    fun getAll(): Flowable<ResultState<List<UiModel>>> =
        Flowable.concatDelayError(listOf(loadSavedData(), fetchAllRemotely()))
            .onErrorReturn {
                ResultState.Error(it, null)
            }

    private fun fetchAllRemotely(): Flowable<ResultState<List<UiModel>>> =
        service.fetchAll()
            .map { dtoList -> saver.persist(dtoList.map(dtoToDbMapper)) }
            .flatMap { saver.loadAll() }.toFlowable()
            .map { dbObjects -> dbObjects.map { dbToUiModelMapper(it) } }
            .map { ResultState.Success(it) }

    private fun loadSavedData(): Flowable<ResultState<List<UiModel>>> {
        val cachedData = saver.loadAll().toFlowable()
        return cachedData.map { ResultState.Loading(it.map(dbToUiModelMapper)) }
    }
}