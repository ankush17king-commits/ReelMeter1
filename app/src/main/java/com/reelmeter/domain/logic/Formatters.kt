package com.reelmeter.domain.logic

object Formatters {
    /** 45 -> "45s", 600 -> "10m", 4500 -> "1h 15m" */
    fun duration(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        val minutes = seconds / 60
        if (minutes < 60) return "${minutes}m"
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0L) "${hours}h" else "${hours}h ${rest}m"
    }
}
