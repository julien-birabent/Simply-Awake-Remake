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

    override fun deleteAll(): Boolean {
        if (!tracksDir.exists()) return true

        val files = tracksDir.listFiles() ?: return true

        var allDeleted = true

        files.forEach { file ->
            if (file.isFile) {
                val deleted = file.delete()
                if (!deleted) {
                    allDeleted = false
                }
            }
        }
        return allDeleted
    }

    override fun count(): Int {
        if (!tracksDir.exists()) return 0
        val files = tracksDir.listFiles() ?: return 0
        return files.count { it.isFile }
    }

    override fun totalSizeBytes(): Long {
        if (!tracksDir.exists()) return 0L
        val files = tracksDir.listFiles() ?: return 0L
        return files
            .asSequence()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}