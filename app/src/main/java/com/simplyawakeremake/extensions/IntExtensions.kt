package com.simplyawakeremake.extensions


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Int.toShortDateLabel(): String {
    if (this == 0) return ""

    val millis = this.toLong() * 1000L
    val date = Date(millis)

    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(date)
}
