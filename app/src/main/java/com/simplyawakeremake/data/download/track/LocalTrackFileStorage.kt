package com.simplyawakeremake.data.download.track

import android.content.Context
import android.os.Environment
import com.simplyawakeremake.data.download.FileStorage
import java.io.File

class LocalTrackFileStorage(context: Context) : FileStorage {
    private val parentMusicDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!
    private val tracksDir: File = File(parentMusicDir, "Meditations")

    override fun getTrackFile(trackId: String): File {
        return File(tracksDir, "$trackId.mp3")
    }

    override fun ensureDirectoriesExist() {
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
    }
}