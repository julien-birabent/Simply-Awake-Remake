package com.example.simplyawakeremake.data.common

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

abstract class DataRepository<UiModel, DTO, DB> {

    protected abstract val fetchAllCall: () -> Single<List<DTO>>
    protected abstract val saver: DataSaver<DB>
    protected abstract val dtoToDbMapper: (DTO) -> DB
    protected abstract val dbToUiModelMapper: (DB) -> UiModel

    fun getAll(): Flowable<ResultState<List<UiModel>>> =
        Flowable.concatDelayError(listOf(loadSavedData(), fetchAllRemotely()))
            .onErrorResumeNext { throwable ->
                loadSavedData().map { resultState ->
                    when {
                        resultState is ResultState.Success && resultState.data.isNotEmpty() -> {
                            resultState
                        }

                        else -> {
                            ResultState.Error(throwable, null)
                        }
                    }
                }
            }

    private fun fetchAllRemotely(): Flowable<ResultState<List<UiModel>>> =
        fetchAllCall()
            .map { dtoList -> saver.persist(dtoList.map(dtoToDbMapper)) }
            .flatMap { saver.loadAll() }.toFlowable()
            .map { dbObjects -> dbObjects.map { dbToUiModelMapper(it) } }
            .map { ResultState.Success(it) }

    private fun loadSavedData(): Flowable<ResultState<List<UiModel>>> {
        val cachedData = saver.loadAll().toFlowable()
        return cachedData.map {
            when {
                it.isEmpty() -> {
                    ResultState.Loading(null)
                }

                else -> {
                    ResultState.Success(it.map(dbToUiModelMapper))
                }
            }
        }
    }
}