package com.simplyawakeremake.data.common

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

abstract class DataRepository<UiModel, DTO, DB> {

    protected abstract val fetchAllCall: suspend () -> List<DTO>
    protected abstract val saver: DataSaver<DB>
    protected abstract val dtoToDbMapper: (DTO) -> DB
    protected abstract val dbToUiModelMapper: (DB) -> UiModel

    private val TAG = this::class.qualifiedName

    fun getAll(): Flow<ResultState<List<UiModel>>> = flow {
        val savedData = loadSavedData()
        emit(savedData)

        try {
            emit(fetchAllRemotely())
            Log.d(TAG, "Data")
        } catch (e: Exception) {
            Log.e(
                TAG,
                "An exception occured while fetching ${e.message}"
            )
            emit(
                if (savedData is ResultState.Success && savedData.data.isNotEmpty()) {
                    Log.d(TAG, "Error while fetching. Return cached data")
                    savedData
                } else {
                    Log.d(TAG, "Error while fetching, no cache data available; returning error")
                    ResultState.Error(e, null)
                }
            )
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchAllRemotely(): ResultState<List<UiModel>> {
        Log.d(TAG, "Fetching all tracks remotely")
        val fetchedTracks = fetchAllCall()
        val trackSaved = fetchedTracks.map(dtoToDbMapper)
        saver.persist(trackSaved)
        return ResultState.Success(trackSaved.map(dbToUiModelMapper))
    }

    private suspend fun loadSavedData(): ResultState<List<UiModel>> {
        val cachedData = saver.loadAll()
        return when {
            cachedData.isEmpty() -> {
                Log.d(TAG, "Cache is empty")
                ResultState.Loading(null)
            }

            else -> {
                Log.d(TAG, "Loading cache data")
                ResultState.Success(cachedData.map(dbToUiModelMapper))
            }
        }
    }
}