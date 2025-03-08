package com.simplyawakeremake.data.download

import java.io.File

interface DownloadService {
    fun enqueueDownload(url: String, destination: File, title: String, onCancel : () -> Unit, onComplete: (File?) -> Unit)
    fun cancelDownloads()
    fun isDownloadComplete(id: Long): Boolean
    fun getDownloadedFile(id: Long): File?
}