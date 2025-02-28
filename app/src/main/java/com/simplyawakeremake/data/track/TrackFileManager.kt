package com.simplyawakeremake.data.track

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.simplyawakeremake.UiTrack
import java.io.File

class TrackFileManager(
    private val context: Context,
    private val trackUriProvider: TrackUriProvider
) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableMapOf<Long, (File?) -> Unit>() // Map downloadId to callback

    init {
        // Register broadcast receiver to listen for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                downloads[id]?.invoke(getDownloadedFile(id))
                downloads.remove(id)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Returns the URI of the downloaded file if available, otherwise returns the streaming URL.
     */
    fun getTrackUri(track: UiTrack): Uri {
        val localFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${track.id}.mp3")
        return if (localFile.exists()) {
            Uri.fromFile(localFile) // Use local file URI
        } else {
            Uri.parse(trackUriProvider.trackUri(track.id)) // Use streaming URL
        }
    }

    private fun isTrackDownloaded(track: UiTrack): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${track.id}.mp3")
        return file.exists()
    }

    /**
     * Downloads a single track if not already downloaded.
     */
    fun downloadTrack(track: UiTrack, onComplete: (File?) -> Unit) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${track.id}.mp3")

        if (file.exists()) {
            onComplete(file)
            return
        }

        val url = trackUriProvider.trackUri(track.id)
        val request = DownloadManager.Request(Uri.parse(url))
            .setDestinationUri(Uri.fromFile(file))
            .setTitle(track.displayName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

        val downloadId = downloadManager.enqueue(request)
        downloads[downloadId] = onComplete
    }

    /**
     * Downloads all tracks in the playlist when the user requests.
     */
    fun downloadAllTracks(tracks: List<UiTrack>, onComplete: (List<File>) -> Unit) {
        val downloadedFiles = mutableListOf<File>()

        tracks.forEach { track ->
            downloadTrack(track) { file ->
                file?.let { downloadedFiles.add(it) }
                if (downloadedFiles.size == tracks.size) {
                    onComplete(downloadedFiles)
                }
            }
        }
    }

    fun deleteTrack(track: UiTrack): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "${track.id}.mp3")
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun deleteAllTracks(): Boolean {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (musicDir == null || !musicDir.exists()) {
            return false
        }

        val files = musicDir.listFiles()
        if (files.isNullOrEmpty()) {
            return false
        }

        var success = true
        files.forEach { file ->
            if (file.isFile && file.name.endsWith(".mp3")) {
                if (!file.delete()) {
                    success = false
                }
            }
        }
        return success
    }

    private fun getDownloadedFile(downloadId: Long): File? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        return downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (columnIndex != -1) {
                    val fileUri = cursor.getString(columnIndex)
                    return File(Uri.parse(fileUri).path ?: return null)
                }
            }
            null
        }
    }
}
