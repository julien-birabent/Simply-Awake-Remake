import android.net.Uri
import com.simplyawakeremake.data.download.DownloadService
import com.simplyawakeremake.data.download.FileStorage
import com.simplyawakeremake.data.download.track.TrackFileManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class TrackFileManagerTest {

    private lateinit var downloadService: DownloadService
    private lateinit var fileStorage: FileStorage
    private lateinit var trackFileManager: TrackFileManager

    @Before
    fun setUp() {
        mockkStatic(Uri::class)

        every { Uri.parse(any()) } returns mockk()
        every { Uri.fromFile(any()) } returns mockk()

        downloadService = mockk(relaxed = true)
        fileStorage = mockk()
        every { fileStorage.ensureDirectoriesExist() } just Runs
        trackFileManager = TrackFileManager(downloadService, fileStorage) { id -> "https://stream/$id.mp3" }

        every { fileStorage.getTrackFile(any()) } answers {
            val id = firstArg<String>()
            mockk<File>(relaxed = true) {
                every { exists() } returns false
                every { name } returns "$id.mp3"
            }
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `getTrackUri returns local file URI if file exists`() {
        val file = mockk<File> {
            every { exists() } returns true
        }
        every { fileStorage.getTrackFile("track1") } returns file

        val uri = trackFileManager.getTrackUri("track1")

        assertEquals(Uri.fromFile(file), uri)
    }

    @Test
    fun `getTrackUri returns streaming URI if file does not exist`() {
        val file = mockk<File> {
            every { exists() } returns false
        }
        every { fileStorage.getTrackFile("track1") } returns file

        val uri = trackFileManager.getTrackUri("track1")

        assertEquals(Uri.parse("https://stream/track1.mp3"), uri)
    }

    @Test
    fun `downloadTracks throws if session already exists`() {
        val fakeCallback: () -> Unit = {}
        val track = "track1" to "Track One"

        every { fileStorage.getTrackFile("track1") } returns mockk {
            every { exists() } returns false
        }

        trackFileManager.downloadTracks(listOf(track), fakeCallback, {}, {})
        assertFailsWith<IllegalStateException> {
            trackFileManager.downloadTracks(listOf(track), fakeCallback, {}, {})
        }
    }

    @Test
    fun `downloadTracks completes immediately if all tracks already exist`() {
        val track = "track1" to "Track One"
        val file = mockk<File> {
            every { exists() } returns true
        }
        every { fileStorage.getTrackFile("track1") } returns file

        var completedFiles: List<File>? = null

        trackFileManager.downloadTracks(
            tracks = listOf(track),
            onDownloadCanceled = {},
            onEachTrackDownloaded = {},
            onComplete = { completedFiles = it }
        )

        assertEquals(listOf(file), completedFiles)
    }

    @Test
    fun `cancelAllDownloads clears session and calls cancelDownloads`() {
        every { downloadService.cancelDownloads() } just Runs

        trackFileManager.downloadTracks(
            listOf("t1" to "Track"),
            {},
            {},
            {}
        )
        trackFileManager.cancelAllDownloads()

        verify { downloadService.cancelDownloads() }
    }

    @Test
    fun `downloadTracks triggers onEachTrackDownloaded and batches correctly`() {
        val tracks = listOf(
            "t1" to "Track 1",
            "t2" to "Track 2",
            "t3" to "Track 3",
            "t4" to "Track 4",
            "t5" to "Track 5"
        )

        val progressUpdates = mutableListOf<Int>()
        val completedFiles = mutableListOf<File>()

        every { downloadService.enqueueDownload(any(), any(), any(), any(), captureLambda()) } answers {
            val file = arg<File>(1)
            lambda<(File?) -> Unit>().invoke(file)
        }

        trackFileManager.downloadTracks(
            tracks = tracks,
            onDownloadCanceled = {},
            onEachTrackDownloaded = { progress -> progressUpdates.add(progress) },
            onComplete = { completedFiles.addAll(it) }
        )

        assertEquals(5, completedFiles.size)
        assertTrue(progressUpdates.isNotEmpty())
        assertTrue(progressUpdates.last() == 100)
    }

    @Test
    fun `downloadTracks with 2 files and batch size 4 completes in one batch`() {
        val tracks = listOf(
            "a" to "Track A",
            "b" to "Track B"
        )

        val completedFiles = mutableListOf<File>()

        every { downloadService.enqueueDownload(any(), any(), any(), any(), captureLambda()) } answers {
            val file = arg<File>(1)
            lambda<(File?) -> Unit>().invoke(file)
        }

        trackFileManager.downloadTracks(
            tracks = tracks,
            onDownloadCanceled = {},
            onEachTrackDownloaded = {},
            onComplete = { completedFiles.addAll(it) }
        )

        assertEquals(2, completedFiles.size)
    }
}
