package com.example.realtimeapplication.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun formatLastSeen(timestamp: Long): String {
        if (timestamp == 0L) return "Recently"
        
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return when {
            isSameDay(now, time) -> "Today at ${formatTime(timestamp)}"
            isYesterday(now, time) -> "Yesterday at ${formatTime(timestamp)}"
            isWithinAWeek(now, time) -> "${getDayOfWeek(time)} at ${formatTime(timestamp)}"
            else -> formatDate(timestamp)
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun getDayOfWeek(calendar: Calendar): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, then: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { 
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, then)
    }

    private fun isWithinAWeek(now: Calendar, then: Calendar): Boolean {
        val weekAgo = Calendar.getInstance().apply { 
            add(Calendar.DAY_OF_YEAR, -7)
        }
        return then.after(weekAgo)
    }
}
