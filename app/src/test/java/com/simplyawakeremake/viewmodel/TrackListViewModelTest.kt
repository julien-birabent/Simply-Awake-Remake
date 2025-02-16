package com.simplyawakeremake.viewmodel

import android.app.Application
import com.simplyawakeremake.MainCoroutineRule
import com.simplyawakeremake.UiTrackTestData
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.track.TrackRepository
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.TestScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit


@ExperimentalCoroutinesApi
class TrackListViewModelTest {

    private lateinit var viewModel: TrackListViewModel
    private val application: Application = mockk(relaxed = true)
    private val trackRepository: TrackRepository = mockk(relaxed = true)

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
        viewModel = TrackListViewModel(application)
    }

    @Test
    fun `test GIVEN a list of tracks THEN screenState is observed THEN first Loading is emitted, secondly the tracks are emitted`() = runTest {
        // Given
        val testTracks = UiTrackTestData.listOfTracks

        every { trackRepository.getAll() } returns Flowable.just(ResultState.Loading(null), ResultState.Success(testTracks))

        // When
        val testObserver = viewModel.screenState.test()

        // Then
        testObserver.assertValues(
            PlayerListUIState.Loading,
            PlayerListUIState.Tracks(testTracks)
        )
    }

    @Test
    fun `test GIVEN a list of tracks not sorted THEN screenState is observed and the tracks are emitted THEN the tracks emitted are sorted by their ordinal`() = runTest {
        // Given
        val testTracks = UiTrackTestData.listOfTracks.reversed()

        every { trackRepository.getAll() } returns Flowable.just(ResultState.Loading(null), ResultState.Success(testTracks))

        // When
        val testObserver = viewModel.screenState.test()

        // Then
        testObserver.assertValueAt(1) { playerListUIState ->
            (playerListUIState as PlayerListUIState.Tracks).items.sortedBy { it.ordinal } == testTracks.sortedBy { it.ordinal }
        }
    }

    @Test
    fun `test GIVEN the tracks are fetched WHEN an error is caught at any point THEN track retrieval fails`() = runTest {
        // Given
        val error = UnknownHostException("UnknownHostException")
        every { trackRepository.getAll() } returns Flowable.just(ResultState.Error(error, null))

        // When
        val testObserver = viewModel.screenState.test()

        // Then
        testObserver.assertValues(
            PlayerListUIState.Error(error)
        )
    }

    @Test
    fun `test GIVEN the tracks are fetched and an error happens WHEN the retry method is called and no error happens ths time THEN track retrieval works`() = runTest {
        // Given
        val error = UnknownHostException("UnknownHostException")
        every { trackRepository.getAll() } returns Flowable.just(ResultState.Error(error, null))

        val testObserver = viewModel.screenState.test()
        testObserver.assertValues(
            PlayerListUIState.Error(error)
        )
        val testTracks = UiTrackTestData.listOfTracks.reversed()
        every { trackRepository.getAll() } returns Flowable.just(ResultState.Loading(null), ResultState.Success(testTracks))

        // WHEN
        viewModel.retryLoadingPlaylist()

        //THEN
        testObserver.assertValueCount(3)
        testObserver.assertValueAt(1) { it is PlayerListUIState.Loading}
        testObserver.assertValueAt(2) { it is PlayerListUIState.Tracks}
    }

    @Test
    fun `test GIVEN multiple errors WHEN retry is called THEN track retrieval works after multiple failures`() = runTest {
        // Given
        val error = UnknownHostException("Network issue")
        every { trackRepository.getAll() } returnsMany listOf(
            Flowable.just(ResultState.Error(error, null)),  // First attempt fails
            Flowable.just(ResultState.Error(error, null)),  // Second attempt fails
            Flowable.just(ResultState.Loading(null), ResultState.Success(UiTrackTestData.listOfTracks)) // Third attempt succeeds
        )

        val testObserver = viewModel.screenState.test()
        testObserver.assertValue(
            PlayerListUIState.Error(error) // Initial failure
        )

        // WHEN
        viewModel.retryLoadingPlaylist()
        viewModel.retryLoadingPlaylist()

        // THEN
        testObserver.assertValueCount(4)
        testObserver.assertValueAt(2) { it is PlayerListUIState.Loading }
        testObserver.assertValueAt(3) { it is PlayerListUIState.Tracks }
    }

    @Test
    fun `test GIVEN a delayed response THEN screenState first emits Loading THEN emits tracks`() = runTest {
        // Create a TestScheduler to control time manually
        val testScheduler = TestScheduler()

        // Given
        every { trackRepository.getAll() } returns Flowable.concat(
            Flowable.just(ResultState.Loading(null)).delay(500, TimeUnit.MILLISECONDS, testScheduler),
            Flowable.just(ResultState.Success(UiTrackTestData.listOfTracks))
        )

        val testObserver = viewModel.screenState.test()
        testObserver.assertEmpty()

        // WHEN: Manually advance time to trigger delayed emission
        testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)
        testObserver.assertValueAt(0) { it is PlayerListUIState.Loading }

        // WHEN: Advance time further to get the Success state
        testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS)

        // THEN Tracks are emitted
        testObserver.assertValueAt(1) { it is PlayerListUIState.Tracks }
    }



    @Test
    fun `test GIVEN an empty list of tracks THEN screenState emits Loading then empty track list`() = runTest {
        // Given
        every { trackRepository.getAll() } returns Flowable.just(ResultState.Loading(null), ResultState.Success(emptyList()))

        // When
        val testObserver = viewModel.screenState.test()

        // Then
        testObserver.assertValues(
            PlayerListUIState.Loading,
            PlayerListUIState.Tracks(emptyList())
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}
