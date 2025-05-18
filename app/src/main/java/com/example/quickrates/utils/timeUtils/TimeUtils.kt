package com.example.quickrates.utils.timeUtils

import java.util.Calendar

object TimeUtils {
    fun validateHour(hour: Int, minute: Int): Boolean{
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) == minute
    }
}