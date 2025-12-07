package com.simplyawakeremake.data.download

import java.io.File

interface DownloadService {
    fun enqueueDownload(
        url: String,
        destination: File,
        title: String,
        onCancel: () -> Unit,
        onComplete: (File?) -> Unit
    )

    fun cancelDownloads()
    fun cancelDownload(file: File)
}