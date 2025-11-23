package com.simplyawakeremake.data.history

import com.simplyawakeremake.data.common.DataSaver
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.ui.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

const val MAX_AMOUNT_STORED_DEFAULT = 20

class TrackHistoryRepository : TrackHistoryRepositoryInterface, KoinComponent {

    override val maxAmountStored: Int = MAX_AMOUNT_STORED_DEFAULT

    private val saver: DataSaver<UiTrackHistory> by inject(qualifier = named("track_history"))

    override fun getRecentlyPlayedHistory(historyLimit: Int): Flow<ResultState<List<UiTrackHistory>>> =
        flow {
            emit(ResultState.Loading(emptyList()))
            emit(ResultState.Success(saver.loadAll()))
        }.catch { emit(ResultState.Error(it, null)) }

    override suspend fun addToHistory(track: Track) = withContext(Dispatchers.IO) {
        val currentHistory = saver.loadAll()
        val newHistory = (listOf(UiTrackHistory(track, System.currentTimeMillis())) + currentHistory).take(maxAmountStored - 1)
        saver.persist(newHistory)
    }
}