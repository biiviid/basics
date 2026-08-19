package com.basicsapp.timer.utils

object TimeFormatter {
    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val millis = ms % 1000
        return "$minutes:${String.format("%02d", seconds)}.${String.format("%03d", millis)}"
    }

    fun formatTimeWithPenalty(ms: Long, penalty: Int): String {
        val adjustedMs = when (penalty) {
            2 -> ms + 2000
            else -> ms
        }
        return formatTime(adjustedMs)
    }

    fun formatTimeForDisplay(ms: Long?, penalty: Int = 0): String {
        if (ms == null) return "N/A"
        return when (penalty) {
            -1 -> "DNF"
            2 -> formatTime(ms + 2000)
            else -> formatTime(ms)
        }
    }
}
