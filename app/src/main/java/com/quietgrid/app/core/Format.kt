package com.quietgrid.app.core

fun formatElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}
