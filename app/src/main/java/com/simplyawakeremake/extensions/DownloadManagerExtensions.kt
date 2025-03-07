package com.simplyawakeremake.extensions

import android.app.DownloadManager
import android.net.Uri
import java.io.File

fun DownloadManager.isDownloadComplete(downloadId: Long): Boolean {
    val query = DownloadManager.Query().setFilterById(downloadId)
    query(query).use { cursor ->
        return if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        } else false
    }
}

fun DownloadManager.getDownloadedFile(downloadId: Long): File? {
    val query = DownloadManager.Query().setFilterById(downloadId)
    return query(query).use { cursor ->
        cursor.takeIf { it.moveToFirst() }
            ?.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            ?.let { Uri.parse(it).path }
            ?.let { File(it) }
    }
}

fun DownloadManager.deleteFileByDownloadId(downloadId: Long): Boolean {
    getDownloadedFile(downloadId)?.takeIf { it.exists() }?.delete()
    remove(downloadId)
    return true
}
