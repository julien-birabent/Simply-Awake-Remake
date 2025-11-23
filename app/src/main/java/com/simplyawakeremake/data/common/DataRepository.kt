package com.simplyawakeremake.data.common

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

abstract class DataRepository<Domain, DTO, DB> {

    protected abstract val fetchAllCall: suspend () -> List<DTO>
    protected abstract val saver: DataSaver<DB>
    protected abstract val dtoToDomainMapper: (DTO) -> Domain
    protected abstract val domainToDbMapper: (Domain) -> DB
    protected abstract val dbToDomainMapper: (DB) -> Domain

    private val TAG = this::class.qualifiedName

    private fun <T> cacheThenNetwork(
        loadCached: suspend () -> ResultState<List<T>>,
        fetchRemote: suspend () -> ResultState<List<T>>,
        shouldEmitRemote: (cached: ResultState<List<T>>, remote: ResultState<List<T>>) -> Boolean
    ): Flow<ResultState<List<T>>> = flow {
        val cached = loadCached()
        emit(cached)

        try {
            val remote = fetchRemote()

            if (shouldEmitRemote(cached, remote)) {
                Log.d(TAG, "Should emit remote true")
                emit(remote)
                Log.d(TAG, "Data refresh completed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "An exception occured while fetching ${e.message}", e)
            val fallback = if (cached is ResultState.Success && cached.data.isNotEmpty()) {
                cached
            } else {
                ResultState.Error(e, null)
            }
            if (fallback is ResultState.Success) {
                Log.d(TAG, "Error while fetching, cached data available; emitting success with cache")
                emit(fallback)
            }else {
                Log.d(TAG, "Error while fetching, no cached data available; emitting error")
            }
        }
    }

    fun getAll(): Flow<ResultState<List<Domain>>> =
        cacheThenNetwork(
            loadCached = { loadSavedData() },
            fetchRemote = { fetchAllRemotely() },
            shouldEmitRemote = { cached, remote ->
                if (cached is ResultState.Success && remote is ResultState.Success) {
                    !haveSameDistinctElements(cached.data, remote.data)
                } else {
                    true
                }
            }
        ).flowOn(Dispatchers.IO)

    private suspend fun fetchAllRemotely(): ResultState<List<Domain>> {
        Log.d(TAG, "Fetching all data remotely")
        val fetchedDtos = fetchAllCall()
        val domainModels = fetchedDtos.map(dtoToDomainMapper)
        val dbModels = domainModels.map(domainToDbMapper)
        saver.persist(dbModels)
        return ResultState.Success(dbModels.map(dbToDomainMapper))
    }

    private suspend fun loadSavedData(): ResultState<List<Domain>> {
        val cachedData = saver.loadAll()
        return when {
            cachedData.isEmpty() -> {
                Log.d(TAG, "Cache is empty")
                ResultState.Loading(null)
            }

            else -> {
                Log.d(TAG, "Loading cache data")
                ResultState.Success(cachedData.map(dbToDomainMapper))
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
