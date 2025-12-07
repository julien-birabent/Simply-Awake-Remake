package com.simplyawakeremake.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.simplyawakeremake.extensions.deleteFileByDownloadId
import com.simplyawakeremake.extensions.getDownloadedFile
import com.simplyawakeremake.extensions.isDownloadComplete
import java.io.File

internal data class DownloadUnit(
    val id: Long,
    val file: File,
    val onFileResult: (DownloadUnit, File?) -> Unit
)

class AndroidDownloadService(context: Context) : DownloadService {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableSetOf<DownloadUnit>()

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { handleDownloadCompletion(it) }
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun enqueueDownload(
        url: String,
        destination: File,
        title: String,
        onCancel: () -> Unit,
        onComplete: (File?) -> Unit
    ) {
        val request = DownloadManager.Request(url.toUri())
            .setDestinationUri(Uri.fromFile(destination))
            .setTitle(title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadId = downloadManager.enqueue(request)
        DownloadUnit(downloadId, destination) { downloadUnit, resultFile ->
            if (resultFile == null) {
                onCancel()
            } else {
                onComplete(resultFile)
            }
            removeUnit(downloadUnit)
        }.also { downloadUnit ->
            downloads.add(downloadUnit)
        }
    }

    private fun removeUnit(downloadUnit: DownloadUnit) {
        downloads.removeIf { it.id == downloadUnit.id }
    }

    override fun cancelDownloads() {
        val downloadIds = downloads.map { it.id }.toLongArray()
        if (downloadIds.isNotEmpty()) {
            downloadManager.remove(*downloadIds)
            downloads.clear()
        }
    }

    override fun cancelDownload(file: File) {
        downloads.find { it.file == file }?.let { toCancel ->
            downloadManager.remove(toCancel.id)
            removeUnit(toCancel)
        }
    }

    private fun handleDownloadCompletion(intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) return

        if (downloadManager.isDownloadComplete(id)) {
            downloadManager.getDownloadedFile(id)
        } else {
            downloadManager.deleteFileByDownloadId(id)
            null
        }.also { file ->
            downloads.find { it.id == id }?.let {
                it.onFileResult(it, file)
            }
        }
    }
}
