package com.example.quickrates.utils.timeUtils

import java.util.Calendar

object TimeUtils {
    fun validateHour(hour: Int, minute: Int): Boolean{
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) == minute
    }

    fun calculateInitialDelay(hour: Int, minute: Int):Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis - System.currentTimeMillis()
    }
}