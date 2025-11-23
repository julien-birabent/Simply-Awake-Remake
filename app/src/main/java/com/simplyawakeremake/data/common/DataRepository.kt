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
        val savedResult: ResultState<List<UiModel>> = loadSavedData()
        emit(savedResult)

        try {
            val remoteResult: ResultState<List<UiModel>> = fetchAllRemotely()

            val shouldEmitRemote = when {
                savedResult is ResultState.Success &&
                        remoteResult is ResultState.Success -> {
                    val cachedList = savedResult.data
                    val remoteList = remoteResult.data

                    val sameContent = haveSameDistinctElements(cachedList, remoteList)
                    if (sameContent) {
                        Log.d(TAG, "Remote data identical to cached data, skipping emission")
                    }
                    !sameContent
                }
                else -> true
            }

            if (shouldEmitRemote) {
                emit(remoteResult)
            }

            Log.d(TAG, "Data refresh completed")
        } catch (e: Exception) {
            Log.e(TAG, "An exception occured while fetching ${e.message}", e)

            val savedIsUsable =
                savedResult is ResultState.Success && savedResult.data.isNotEmpty()

            if (savedIsUsable) {
                Log.d(TAG, "Error while fetching. Cached data already emitted, keeping it")
            } else {
                Log.d(TAG, "Error while fetching, no cached data available; emitting error")
                emit(ResultState.Error(e, null))
            }
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

    private fun <T> haveSameDistinctElements(
        first: List<T>,
        second: List<T>
    ): Boolean {
        return first.toSet() == second.toSet()
    }

}