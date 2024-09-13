package com.example.simplyawakeremake.extensions

fun Long.fromMsToMinuteSeconds(): String {
    val minutes = this / 1000 / 60
    val seconds = this / 1000 % 60
    return "${minutes}:${seconds}"
}