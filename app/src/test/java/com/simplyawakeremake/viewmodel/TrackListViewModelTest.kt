package com.simplyawakeremake.viewmodel

import android.app.Application
import com.simplyawakeremake.MainCoroutineRule
import com.simplyawakeremake.UiTrack
import com.simplyawakeremake.UiTrackTestData
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import java.net.UnknownHostException

@ExperimentalCoroutinesApi
class TrackListViewModelTest {

    private lateinit var viewModel: TrackListViewModel
    private val application: Application = mockk(relaxed = true)
    private val trackRepository: TrackRepositoryInterface = mockk(relaxed = true)

    @get:Rule
    val testCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        startKoin {
            modules(
                module {
                    single { trackRepository }
                }
            )
        }
        viewModel = TrackListViewModel(application, trackRepository)
    }

    @Test
    fun `test GIVEN a list of tracks THEN screenState is observed THEN first Loading is emitted, secondly the tracks are emitted`() = runTest {
        // Given
        val testTracks = UiTrackTestData.listOfTracks

        coEvery { trackRepository.getAllTracks() } returns flowOf(ResultState.Loading(null), ResultState.Success(testTracks))

        // When
        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()

        // Then
        assertEquals(listOf(PlayerListUIState.Loading, PlayerListUIState.Tracks(testTracks)), stateList)
        job.cancel()
    }

    @Test
    fun `test GIVEN a list of tracks not sorted THEN screenState is observed and the tracks are emitted sorted`() = runTest {
        // Given
        val testTracks = UiTrackTestData.listOfTracks.reversed()

        coEvery { trackRepository.getAllTracks() } returns flowOf(ResultState.Loading(null), ResultState.Success(testTracks))

        // When
        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()

        // Then
        val sortedTracks = testTracks.sortedBy { it.ordinal }
        assertEquals(PlayerListUIState.Tracks(sortedTracks), stateList.last())
        job.cancel()
    }

    @Test
    fun `test GIVEN the tracks are fetched WHEN an error occurs THEN track retrieval fails`() = runTest {
        // Given
        val error = UnknownHostException("UnknownHostException")
        coEvery { trackRepository.getAllTracks() } returns flowOf(ResultState.Error(error, null))

        // When
        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()
        // Then
        assertEquals(PlayerListUIState.Error(error), stateList.last())
        job.cancel()
    }

    @Test
    fun `test GIVEN an error WHEN retry is called THEN track retrieval succeeds`() = runTest {
        // Given
        val error = UnknownHostException("UnknownHostException")
        val testTracks = UiTrackTestData.listOfTracks

        val testFlow = MutableStateFlow<ResultState<List<UiTrack>>>(ResultState.Error(error, null))

        coEvery { trackRepository.getAllTracks() } coAnswers { testFlow }

        testFlow.value = ResultState.Loading(null)
        testFlow.value = ResultState.Error(error, null)

        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }

        advanceUntilIdle()
        assertTrue(stateList.any { it is PlayerListUIState.Error })

        // WHEN: Retry
        testFlow.value = ResultState.Loading(null)
        testFlow.value = ResultState.Success(testTracks)
        viewModel.retryLoadingPlaylist()
        advanceUntilIdle()

        assertEquals(PlayerListUIState.Tracks(testTracks), stateList.last())
        job.cancel()
    }


    @Test
    fun `test GIVEN multiple errors WHEN retry is called THEN track retrieval works after failures`() = runTest {
        // Given
        val error = UnknownHostException("Network issue")
        coEvery { trackRepository.getAllTracks() } returns flow {
            emit(ResultState.Error(error, null)) // First attempt fails
            emit(ResultState.Error(error, null)) // Second attempt fails
            emit(ResultState.Loading(null)) // Third attempt starts
            emit(ResultState.Success(UiTrackTestData.listOfTracks)) // Third attempt succeeds
        }

        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()
        // WHEN
        viewModel.retryLoadingPlaylist()
        viewModel.retryLoadingPlaylist()

        // THEN
        assertEquals(PlayerListUIState.Tracks(UiTrackTestData.listOfTracks), stateList.last())
        job.cancel()
    }

    @Test
    fun `test GIVEN a delayed response THEN screenState first emits Loading THEN emits tracks`() = runTest {
        // Given
        coEvery { trackRepository.getAllTracks() } returns flow {
            emit(ResultState.Loading(null))
            kotlinx.coroutines.delay(500)
            emit(ResultState.Success(UiTrackTestData.listOfTracks))
        }

        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()
        // THEN
        assertEquals(PlayerListUIState.Loading, stateList[0])

        // Advance virtual time
        kotlinx.coroutines.delay(500)

        assertEquals(PlayerListUIState.Tracks(UiTrackTestData.listOfTracks), stateList.last())
        job.cancel()
    }

    @Test
    fun `test GIVEN an empty list of tracks THEN screenState emits Loading then empty track list`() = runTest {
        // Given
        coEvery { trackRepository.getAllTracks() } returns flowOf(ResultState.Loading(null), ResultState.Success(emptyList()))

        // When
        val stateList = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(stateList)
        }
        advanceUntilIdle()
        // Then
        assertEquals(PlayerListUIState.Tracks(emptyList()), stateList.last())
        job.cancel()
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}
