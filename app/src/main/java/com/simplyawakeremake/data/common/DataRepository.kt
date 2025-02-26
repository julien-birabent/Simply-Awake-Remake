package com.simplyawakeremake.data.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

abstract class DataRepository<UiModel, DTO, DB> {

    protected abstract val fetchAllCall: suspend () -> List<DTO>
    protected abstract val saver: DataSaver<DB>
    protected abstract val dtoToDbMapper: (DTO) -> DB
    protected abstract val dbToUiModelMapper: (DB) -> UiModel

    fun getAll(): Flow<ResultState<List<UiModel>>> = flow {
        val savedData = loadSavedData()
        emit(savedData)

        try {
            emit(fetchAllRemotely())
        } catch (e: Exception) {
            emit(
                if (savedData is ResultState.Success && savedData.data.isNotEmpty()) savedData
                else ResultState.Error(e, null)
            )
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchAllRemotely(): ResultState<List<UiModel>> {
        val fetchedTracks = fetchAllCall()
        saver.persist(fetchedTracks.map(dtoToDbMapper))
        return ResultState.Success(saver.loadAll().map(dbToUiModelMapper))
    }

    private suspend fun loadSavedData(): ResultState<List<UiModel>> {
        val cachedData = saver.loadAll()
        return when {
            cachedData.isEmpty() -> {
                ResultState.Loading(null)
            }

            else -> {
                ResultState.Success(cachedData.map(dbToUiModelMapper))
            }
        }
    }
}