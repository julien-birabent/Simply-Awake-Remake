package com.simplyawakeremake.data.download

import io.mockk.mockk
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadSessionTest {

    @Test
    fun `cancel clears pending list and triggers cancel callback`() {
        var canceled = false
        val session = DownloadSession(
            pendingDownloads = mutableListOf("t1" to "Track 1"),
            onDownloadCanceled = { canceled = true },
            onEachDownloaded = {},
            onComplete = {}
        )
        session.cancel()
        assertTrue(canceled)
        assertTrue(session.pendingDownloads.isEmpty())
    }

    @Test
    fun `startNextBatch enqueues correct batch and reduces pending`() {
        val enqueued = mutableListOf<String>()
        val session = DownloadSession(
            pendingDownloads = mutableListOf("a" to "Track A", "b" to "Track B", "c" to "Track C"),
            onDownloadCanceled = {},
            onEachDownloaded = {},
            onComplete = {}
        )

        session.startNextBatch(2) { _, (id, _) -> enqueued += id }

        assertEquals(listOf("a", "b"), enqueued)
        assertEquals(listOf("c" to "Track C"), session.pendingDownloads)
    }

    @Test
    fun `handleDownloadResult updates progress and completes batch`() {
        var progress = 0
        var completed = false
        val fakeFile = mockk<File>(relaxed = true)

        val session = DownloadSession(
            pendingDownloads = mutableListOf("t2" to "Track 2"),
            onDownloadCanceled = {},
            onEachDownloaded = { progress = it },
            onComplete = { completed = true }
        )

        session.startNextBatch(4) { _, _ -> }
        session.handleDownloadResult(fakeFile) {}
        session.startNextBatch(4) { _, _ -> }
        assertEquals(100, progress)
        assertTrue(completed)
    }

    @Test
    fun `handleDownloadResult triggers next batch if no more pending`() {
        var nextBatchCalled = false
        val session = DownloadSession(
            pendingDownloads = mutableListOf("t2" to "Track 2"),
            onDownloadCanceled = {},
            onEachDownloaded = {},
            onComplete = {}
        )

        session.startNextBatch(1) { _, _ -> }
        session.handleDownloadResult(mockk(relaxed = true)) { nextBatchCalled = true }

        assertTrue(nextBatchCalled)
    }

    @Test
    fun `handleDownloadResult triggers next batch if more pending`() {
        var nextBatchCalled = false
        val session = DownloadSession(
            pendingDownloads = mutableListOf("t1" to "Track 1", "t2" to "Track 2"),
            onDownloadCanceled = {},
            onEachDownloaded = {},
            onComplete = {}
        )

        session.startNextBatch(1) { _, _ -> }
        session.handleDownloadResult(mockk(relaxed = true)) { nextBatchCalled = true }

        assertTrue(nextBatchCalled)
    }

    @Test
    fun `startNextBatch handles less items than batch size`() {
        val enqueued = mutableListOf<String>()
        val session = DownloadSession(
            pendingDownloads = mutableListOf("t1" to "Track 1", "t2" to "Track 2"),
            onDownloadCanceled = {},
            onEachDownloaded = {},
            onComplete = {}
        )

        session.startNextBatch(4) { _, (id, _) -> enqueued += id }

        assertEquals(listOf("t1", "t2"), enqueued)
        assertTrue(session.pendingDownloads.isEmpty())
    }
}
