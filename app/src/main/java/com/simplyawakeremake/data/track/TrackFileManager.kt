package com.simplyawakeremake.data.track

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

/*
* This class could be more abstract and extensible for further use but as of now it answers to all the
* needs of the app.
* */
class TrackFileManager(
    context: Context,
    private val trackUriProvider: (String) -> String
) {

    private val parentMusicDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!
    private val meditationsDir: File = File(parentMusicDir, "Meditations")
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableMapOf<Long, (File?) -> Unit>()

    init {
        initMeditationFolder()
        // Register broadcast receiver to listen for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.onDownloadCompletionUpdate()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun initMeditationFolder() {
        if (!meditationsDir.exists()) {
            meditationsDir.mkdirs()
        }
    }

    private fun Intent.onDownloadCompletionUpdate() {
        val id = getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) return

        downloads[id]?.invoke(getDownloadedFile(id))
        downloads.remove(id)
    }

    /**
     * Returns the URI of the downloaded file if available, otherwise returns the streaming URL.
     */
    fun getTrackUri(trackId: String): Uri {
        val localFile = File(meditationsDir, "$trackId.mp3")
        return if (localFile.exists()) {
            Uri.fromFile(localFile) // Use local file URI
        } else {
            Uri.parse(trackUriProvider(trackId)) // Use streaming URL
        }
    }

    /**
     * Downloads a single track if not already downloaded.
     */
    private fun downloadTrack(trackId: String, trackTitle: String, onComplete: (File?) -> Unit) {
        val file = File(meditationsDir, "$trackId.mp3")

        if (file.exists()) {
            onComplete(file)
            return
        }

        val url = trackUriProvider(trackId)
        val request = DownloadManager.Request(Uri.parse(url))
            .setDestinationUri(Uri.fromFile(file))
            .setTitle(trackTitle)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                }
            }
        val downloadId = downloadManager.enqueue(request)
        downloads[downloadId] = onComplete
    }

    /**
     * Downloads all tracks in the playlist when the user requests.
     */
    fun downloadTracks(
        tracks: List<Pair<String, String>>,
        onEachTrackDownloaded: (progressPercentage: Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        val downloadedFiles = mutableListOf<File>()
        tracks.forEachIndexed { _, (trackId, trackTitle) ->
            downloadTrack(trackId, trackTitle) { file ->
                file?.let {
                    downloadedFiles.add(it)
                    onEachTrackDownloaded((downloadedFiles.size * 100) / tracks.size)
                }

                if (downloadedFiles.size == tracks.size) {
                    onComplete(downloadedFiles)
                }
            }
        }
    }


    fun deleteTrack(trackId: String): Boolean {
        return File(meditationsDir, "$trackId.mp3")
            .takeIf { it.exists() }
            ?.delete() ?: false
    }

    fun deleteAllTracks(): Boolean {
        return meditationsDir.takeIf { it.exists() }?.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".mp3") }
            .all { it.delete() }
    }

    private fun getDownloadedFile(downloadId: Long): File? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        return downloadManager.query(query).use { cursor ->
            cursor.takeIf { it.moveToFirst() }
                ?.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                ?.takeIf { it != -1 }
                ?.let { cursor.getString(it) }
                ?.let { Uri.parse(it).path }
                ?.let { File(it) }
        }
    }
}
