package com.simplyawakeremake.data.download

import java.io.File

interface FileStorage {
    fun getTrackFile(trackId: String): File
    fun ensureDirectoriesExist()

    fun deleteAll() : Boolean

    fun count() : Int

    fun totalSizeBytes(): Long
}