package com.simplyawakeremake.viewmodel

import com.simplyawakeremake.MainCoroutineRule
import com.simplyawakeremake.ui.model.UiTrack
import com.simplyawakeremake.UiTrackTestData
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepositoryInterface
import com.simplyawakeremake.usecases.CheckTrackDownloadStatusUseCase
import com.simplyawakeremake.usecases.DownloadProgress
import com.simplyawakeremake.usecases.DownloadTrackListUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val trackRepository: TrackRepositoryInterface = mockk(relaxed = true)
    private val downloadTrackListUseCase: DownloadTrackListUseCase = mockk(relaxed = true)
    private val checkTrackDownloadStatusUseCase: CheckTrackDownloadStatusUseCase =
        mockk(relaxed = true)

    @get:Rule
    val testCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        startKoin {
            modules(
                module {
                    single { trackRepository }
                    single { downloadTrackListUseCase }
                    single { checkTrackDownloadStatusUseCase }
                }
            )
        }
        viewModel = TrackListViewModel(
            trackRepository,
            downloadTrackListUseCase,
            checkTrackDownloadStatusUseCase,
            mockk(relaxed = true)
        )
    }

    @Test
    fun `test GIVEN a list of tracks THEN screenState is observed THEN first Loading is emitted, secondly the tracks are emitted`() =
        runTest {
            // Given
            val testTracks = UiTrackTestData.listOfTracks

            coEvery { trackRepository.getAllTracks() } returns flowOf(
                ResultState.Loading(null),
                ResultState.Success(testTracks)
            )

            // When
            val stateList = mutableListOf<PlayerListUIState>()
            val job = launch {
                viewModel.screenState.toList(stateList)
            }
            advanceUntilIdle()

            // Then
            assertEquals(
                listOf(PlayerListUIState.Loading, PlayerListUIState.Tracks(testTracks)),
                stateList
            )
            job.cancel()
        }

    @Test
    fun `test GIVEN a list of tracks not sorted THEN screenState is observed and the tracks are emitted sorted`() =
        runTest {
            // Given
            val testTracks = UiTrackTestData.listOfTracks.reversed()

            coEvery { trackRepository.getAllTracks() } returns flowOf(
                ResultState.Loading(null),
                ResultState.Success(testTracks)
            )

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
    fun `test GIVEN the tracks are fetched WHEN an error occurs THEN track retrieval fails`() =
        runTest {
            // Given
            val error = UnknownHostException("UnknownHostException")
            coEvery { trackRepository.getAllTracks() } returns flowOf(
                ResultState.Error(
                    error,
                    null
                )
            )

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
    fun `test GIVEN multiple errors WHEN retry is called THEN track retrieval works after failures`() =
        runTest {
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
    fun `test GIVEN a delayed response THEN screenState first emits Loading THEN emits tracks`() =
        runTest {
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
    fun `test GIVEN an empty list of tracks THEN screenState emits Loading then empty track list`() =
        runTest {
            // Given
            coEvery { trackRepository.getAllTracks() } returns flowOf(
                ResultState.Loading(null),
                ResultState.Success(emptyList())
            )

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

    @Test
    fun `test downloadAllTracks starts downloading when conditions are met`() = runTest {
        // Given
        val testTracks = UiTrackTestData.listOfTracks
        val expectedStates = mutableListOf(
            DownloadProgress.InProgress(0),
            *List(testTracks.size) { index -> DownloadProgress.InProgress(index) }.toTypedArray(),
            DownloadProgress.Success(listOf())
        )

        coEvery { trackRepository.getAllTracks() } returns flowOf(ResultState.Success(testTracks))

        coEvery { downloadTrackListUseCase.execute(testTracks) } returns flow {
            expectedStates.forEach { emit(it) }
        }

        val collectedStates = mutableListOf<PlayerListUIState>()
        val job = launch {
            viewModel.screenState.toList(collectedStates)
        }

        viewModel.retryLoadingPlaylist()
        advanceUntilIdle()

        assertTrue(collectedStates.last() is PlayerListUIState.Tracks)

        // When
        viewModel.downloadAllTracks()
        advanceUntilIdle()

        // Then
        coVerify { downloadTrackListUseCase.execute(testTracks) }

        // Cleanup
        job.cancel()
    }


    @Test
    fun `test cancelDownload cancels downloads`() = runTest {
        // When
        viewModel.cancelDownload()
        advanceUntilIdle()

        // Then
        coVerify { downloadTrackListUseCase.cancelDownloads() }
    }

    @Test
    fun `test resetDownloadState sets state to Idle`() = runTest {
        // Given
        viewModel.resetDownloadState()
        advanceUntilIdle()

        // Then
        assertEquals(DownloadProgress.Idle, viewModel.downloadState.value)
    }

    @Test
    fun `test isTrackDownloaded returns correct status`() {
        // Given
        val trackId = "track_123"
        every { checkTrackDownloadStatusUseCase.execute(trackId) } returns true

        // When
        val isDownloaded = viewModel.isTrackDownloaded(trackId)

        // Then
        assertTrue(isDownloaded)
    }


    @After
    fun tearDown() {
        stopKoin()
    }
}
