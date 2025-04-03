package com.example.quickrates.utils

import android.icu.text.DecimalFormat
import io.ktor.http.parsing.ParseException

object NumberFormatter {
    private val formatter = DecimalFormat("#,###.##")

    fun format(number: String): String{
        return try {
            val cleanString = number.replace(",", "")
            if (cleanString.isEmpty()){
                return ""
            }
            val parset = formatter.parse(cleanString)?.toLong() ?: 0L
            formatter.format(parset)
        } catch (e: ParseException){
            number
        }
    }
}