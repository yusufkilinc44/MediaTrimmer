package com.yusufkilinc.mediatrimmer.core.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatTimecode(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        val centis = (ms % 1000) / 10

        return String.format(Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
    }

    fun formatTimecodeSec(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return when {
            hours > 0 -> String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format(Locale.US, "%dm %ds", minutes, seconds)
            else -> String.format(Locale.US, "%ds", seconds)
        }
    }

    fun msToSeconds(ms: Long): Double = ms / 1000.0
}
