package com.zaaam.Zmusic.util

import java.util.concurrent.TimeUnit

fun Long.toTimeString(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

fun Long.toHoursMinutesString(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} jam ${minutes} menit"
        hours > 0 -> "${hours} jam"
        else -> "${minutes} menit"
    }
}
