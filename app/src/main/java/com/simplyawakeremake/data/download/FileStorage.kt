package com.simplyawakeremake.data.download

import java.io.File

interface FileStorage {
    fun getTrackFile(trackId: String): File
    fun ensureDirectoriesExist()
}