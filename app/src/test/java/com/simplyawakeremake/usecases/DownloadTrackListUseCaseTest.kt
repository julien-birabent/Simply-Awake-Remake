package com.simplyawakeremake.usecases


import com.simplyawakeremake.UiTrackTestData
import com.simplyawakeremake.data.track.TrackFileManager
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

@ExperimentalCoroutinesApi
class DownloadTrackListUseCaseTest {

    private lateinit var useCase: DownloadTrackListUseCase
    private val trackFileManager: TrackFileManager = mockk(relaxed = true)
    private val testTracks = UiTrackTestData.listOfTracks

    @Before
    fun setup() {
        useCase = DownloadTrackListUseCase(trackFileManager)
    }

    @Test
    fun `test execute emits correct progress states`() = runTest {
        // Given
        val expectedFiles = listOf(File("track1.mp3"), File("track2.mp3"))

        every { trackFileManager.downloadTracks(any(), any(), any(), any()) } answers {
            val onEachTrackDownloaded = arg<(Int) -> Unit>(2)
            val onComplete = arg<(List<File>) -> Unit>(3)
            repeat(testTracks.size) { onEachTrackDownloaded(it + 1) }
            onComplete(expectedFiles)
        }

        // When

        val results = useCase.execute(testTracks).toList()
        advanceUntilIdle()

        // Then
        assertEquals(
            listOf(
                DownloadProgress.InProgress(0),
                *List(testTracks.size) { index -> DownloadProgress.InProgress(index + 1) }.toTypedArray(),
                DownloadProgress.Success(expectedFiles)
            ), results
        )
    }

    @Test
    fun `test execute emits failure on exception`() = runTest {
        // Given
        val exception = RuntimeException("Download failed")
        val expected = listOf(DownloadProgress.InProgress(0), DownloadProgress.Failure(exception))
        every { trackFileManager.downloadTracks(any(), any(), any(), any()) } throws exception

        // When
        val result = useCase.execute(testTracks).toList()

        // Then
        assertEquals(expected, result)
        assertTrue(result.last() is DownloadProgress.Failure)
        assertEquals(exception, (result.last() as DownloadProgress.Failure).error)
    }

    @Test
    fun `test cancelDownloads stops the download process`() = runTest {
        // TODO
    }


    @After
    fun tearDown() {
        clearMocks(trackFileManager)
    }
}
