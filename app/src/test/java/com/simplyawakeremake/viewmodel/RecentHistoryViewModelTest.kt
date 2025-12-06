package com.simplyawakeremake.viewmodel

import app.cash.turbine.test
import com.simplyawakeremake.data.common.ResultState
import com.simplyawakeremake.data.history.UiTrackHistory
import com.simplyawakeremake.ui.history.RecentHistoryViewModel
import com.simplyawakeremake.usecases.history.GetRecentHistoryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class RecentHistoryViewModelTest {

    // TestCoroutineDispatcher and Scope for controlling coroutine execution
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Mock dependencies
    private val getRecentHistoryUseCase: GetRecentHistoryUseCase = mockk()

    // ViewModel instance
    private lateinit var viewModel: RecentHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Use test dispatcher for coroutines
        every { getRecentHistoryUseCase.execute(any()) } returns flowOf(ResultState.Loading(emptyList()))
        viewModel = RecentHistoryViewModel(getRecentHistoryUseCase, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given successful history fetch, When ViewModel loads, Then emits Loading and RecentHistoryLoaded`() = testScope.runTest {
        // Given
        val mockHistory = listOf(
            UiTrackHistory(track = mockk(), playedTimestamp = 1000L),
            UiTrackHistory(track = mockk(), playedTimestamp = 2000L)
        )

        coEvery { getRecentHistoryUseCase.execute(any()) } returns flowOf(ResultState.Success(mockHistory))

        // When
        viewModel = RecentHistoryViewModel(getRecentHistoryUseCase, mockk(relaxed = true))

        // Then
        viewModel.uiState.test {
            assertEquals(RecentHistoryViewModel.RecentHistoryUIState.Loading, awaitItem()) // First state should be Loading
            val loadedState = awaitItem() as RecentHistoryViewModel.RecentHistoryUIState.RecentHistoryLoaded
            assertEquals(2, loadedState.items.size) // Verify history size
            assertTrue(loadedState.items[0].playedTimestamp > loadedState.items[1].playedTimestamp) // Should be sorted by timestamp DESC
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Given error from history fetch, When ViewModel loads, Then emits Loading and Error state`() = testScope.runTest {
        // Given
        val exception = RuntimeException("No history available")
        coEvery { getRecentHistoryUseCase.execute(any()) } returns flowOf(ResultState.Error(exception, null))

        // When
        viewModel = RecentHistoryViewModel(getRecentHistoryUseCase, mockk(relaxed = true))

        // Then
        viewModel.uiState.test {
            assertEquals(RecentHistoryViewModel.RecentHistoryUIState.Loading, awaitItem()) // First state should be Loading
            val errorState = awaitItem() as RecentHistoryViewModel.RecentHistoryUIState.Error
            assertEquals(exception, errorState.throwable) // Ensure correct error
            cancelAndConsumeRemainingEvents()
        }
    }
}
