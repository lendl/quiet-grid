package com.quietgrid.app.core

private const val SECONDS_PER_HOUR = 3600

fun formatElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}

fun formatTotalTime(seconds: Int): String {
    if (seconds < SECONDS_PER_HOUR) return formatElapsed(seconds)
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / 60
    return "${hours}h ${minutes.toString().padStart(2, '0')}m"
}
