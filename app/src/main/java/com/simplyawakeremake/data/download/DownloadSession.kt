package com.simplyawakeremake.data.download

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DownloadSession(
    var pendingDownloads: MutableList<Pair<String, String>>,
    private val onDownloadCanceled: () -> Unit,
    private val onEachDownloaded: (Int) -> Unit,
    private val onComplete: (List<File>) -> Unit
) {
    private val downloadedFiles: MutableList<File> = mutableListOf()
    private var batchCounter = AtomicInteger(0)

    fun cancel() {
        pendingDownloads.clear()
        onDownloadCanceled()
    }

    fun startNextBatch(
        batchSize: Int,
        enqueue: (DownloadSession, Pair<String, String>) -> Unit
    ) {
        if (pendingDownloads.isEmpty()) {
            onComplete(downloadedFiles)
            return
        }

        val batchSizeAdjusted = minimumBatchSizeNeeded(batchSize)
        val batch = pendingDownloads.take(batchSizeAdjusted)

        pendingDownloads = pendingDownloads.drop(batch.size).toMutableList()
        batchCounter = AtomicInteger(batch.size)

        batch.forEach { track -> enqueue(this, track) }
    }

    private fun minimumBatchSizeNeeded(batchSize: Int) =
        if (batchSize > pendingDownloads.size) pendingDownloads.size else batchSize

    fun handleDownloadResult(
        resultFile: File?,
        onBatchComplete: () -> Unit
    ) {
        resultFile?.let {
            downloadedFiles.add(it)
            onEachDownloaded(calculateProgress())
        }

        if (batchCounter.decrementAndGet() <= 0) { onBatchComplete() }

    }

    private fun calculateProgress(): Int {
        val total = downloadedFiles.size + pendingDownloads.size
        return if (total == 0) 100 else (downloadedFiles.size * 100) / total
    }
}

