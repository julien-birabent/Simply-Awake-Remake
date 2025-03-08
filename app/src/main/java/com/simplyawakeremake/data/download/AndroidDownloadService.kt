package com.simplyawakeremake.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import com.simplyawakeremake.extensions.deleteFileByDownloadId
import com.simplyawakeremake.extensions.getDownloadedFile
import com.simplyawakeremake.extensions.isDownloadComplete
import java.io.File

class AndroidDownloadService(context: Context) : DownloadService {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableMapOf<Long, (File?) -> Unit>()

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
        val request = DownloadManager.Request(Uri.parse(url))
            .setDestinationUri(Uri.fromFile(destination))
            .setTitle(title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadId = downloadManager.enqueue(request)
        downloads[downloadId] = { resultFile ->
            if (resultFile == null) {
                onCancel()
            } else onComplete(resultFile)
            downloads.remove(downloadId)
        }
    }

    override fun cancelDownloads() {
        val downloadIds = downloads.keys.toLongArray()
        if (downloadIds.isNotEmpty()) {
            downloadManager.remove(*downloadIds)
            downloads.clear()
        }
    }

    override fun isDownloadComplete(id: Long): Boolean {
        return downloadManager.isDownloadComplete(id)
    }

    override fun getDownloadedFile(id: Long): File? {
        return downloadManager.getDownloadedFile(id)
    }

    private fun handleDownloadCompletion(intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) return

        if (downloadManager.isDownloadComplete(id)) {
            downloadManager.getDownloadedFile(id)
        } else {
            downloadManager.deleteFileByDownloadId(id)
            null
        }.also {
            downloads[id]?.invoke(it)
            downloadManager.remove(id)
        }
    }
}
