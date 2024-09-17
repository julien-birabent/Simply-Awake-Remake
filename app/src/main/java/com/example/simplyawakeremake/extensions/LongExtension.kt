package com.example.simplyawakeremake.extensions

// Format a long to this format "mm:ss"
fun Long.formatToMinuteAndSeconds(): String {
    val minutes = this / 1000 / 60
    val seconds = (this / 1000 % 60).run { if (this < 10) "0${this}" else this }
    return "${minutes}:${seconds}"
}