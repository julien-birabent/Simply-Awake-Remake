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
import com.simplyawakeremake.extensions.deleteFileByDownloadId
import com.simplyawakeremake.extensions.getDownloadedFile
import com.simplyawakeremake.extensions.isDownloadComplete
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
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
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

    fun cancelAllDownloads() {
        val downloadIds = downloads.keys.toLongArray()
        if (downloadIds.isNotEmpty()) {
            downloadManager.remove(*downloadIds)
            downloads.clear()
        }
    }

    private fun initMeditationFolder() {
        if (!meditationsDir.exists()) {
            meditationsDir.mkdirs()
        }
    }

    private fun Intent.onDownloadCompletionUpdate() {
        val id = getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) return

        val file: File? = if (downloadManager.isDownloadComplete(id)) {
            downloadManager.getDownloadedFile(id)
        } else {
            downloadManager.deleteFileByDownloadId(id)
            null
        }
        downloads[id]?.invoke(file)
        downloads.remove(id)
    }

    /**
     * Returns the URI of the downloaded file if available, otherwise returns the streaming URL.
     */
    fun getTrackUri(trackId: String): Uri {
        val localFile = getTrackFile(trackId)
        return if (localFile.exists()) {
            Uri.fromFile(localFile) // Use local file URI
        } else {
            Uri.parse(trackUriProvider(trackId)) // Use streaming URL
        }
    }

    /**
     * Downloads a single track if not already downloaded.
     */
    private fun downloadTrack(
        trackId: String,
        trackTitle: String,
        onDownloadCanceled: () -> Unit,
        onTrackDownloaded: (File?) -> Unit
    ) {
        val file = getTrackFile(trackId)

        if (file.exists()) {
            onTrackDownloaded(file)
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

        downloads[downloadId] = { resultFile ->
            if (resultFile == null) {
                onDownloadCanceled()
            } else onTrackDownloaded(resultFile)
        }
    }

    /**
     * Downloads all tracks in the playlist when the user requests.
     */
    fun downloadTracks(
        tracks: List<Pair<String, String>>,
        onDownloadCanceled: () -> Unit,
        onEachTrackDownloaded: (progressPercentage: Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {

        if (areAllTracksDownloaded(tracks)) {
            onComplete(tracks.map { (trackId, _) -> getTrackFile(trackId) })
            return
        }

        val downloadedFiles = mutableListOf<File>()
        tracks.forEachIndexed { _, (trackId, trackTitle) ->
            downloadTrack(trackId, trackTitle, onDownloadCanceled) { file ->
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

    private fun areAllTracksDownloaded(tracks: List<Pair<String, String>>): Boolean {
        return tracks.all { (trackId, _) -> getTrackFile(trackId).exists() }
    }

    private fun getTrackFile(trackId: String): File {
        return File(meditationsDir, "$trackId.mp3")
    }

    fun deleteTrack(trackId: String): Boolean {
        return getTrackFile(trackId)
            .takeIf { it.exists() }
            ?.delete() ?: false
    }

    fun deleteAllTracks(): Boolean {
        return meditationsDir.takeIf { it.exists() }?.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".mp3") }
            .all { it.delete() }
    }
}
