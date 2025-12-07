package com.simplyawakeremake.extensions

import java.util.Locale


// Format a long to this format "mm:ss"
fun Long.formatToMinuteAndSeconds(): String {
    val minutes = this / 1000 / 60
    val seconds = (this / 1000 % 60).run { if (this < 10) "0${this}" else this }
    return "${minutes}:${seconds}"
}

fun Long.formatSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.getDefault(), "%.0f KB", kb)
        else -> "$this B"
    }
}
