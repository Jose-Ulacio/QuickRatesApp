package com.example.quickrates.utils.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.quickrates.App
import com.example.quickrates.data.model.DollarResponse
import com.example.quickrates.data.model.EuroResponse
import com.example.quickrates.data.remote.api.KtorClient
import com.example.quickrates.utils.notifications.NotificationProvider
import com.example.quickrates.utils.notifications.NotificationUtils
import com.example.quickrates.utils.timeUtils.TimeUtils
import java.util.UUID

class QuickRatesWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            val isBCV = TimeUtils.validateHour(17, 0)
            val isParalelo = TimeUtils.validateHour(12, 30)

            if (isBCV || isParalelo){
                val dollarResponse = getDollar()
                val euroResponse = getEuro()

                val message = if (isBCV){
                    "Dólar: ${dollarResponse.monitorDollars.BCV.price} Bs\nEuro: ${euroResponse.monitorEuros.BCV.price} Bs"
                } else {
                    "Dólar: ${dollarResponse.monitorDollars.enParalelo.price} Bs\nEuro: ${euroResponse.monitorEuros.enParalelo.price} Bs"
                }

                showNotification(message, isBCV)

                return Result.success()
            } else {
                Log.e("WorkerError","Hora No correcta")
                return Result.failure()
            }

        }catch (e: Exception){
            e.printStackTrace()
            return Result.failure()
        }
    }

    suspend fun getDollar():DollarResponse{
        return KtorClient.getDollarRates()
    }

    suspend fun getEuro():EuroResponse{
        return KtorClient.getEuroRates()
    }

    val notificationManager = App.getNotificationManager()

    fun showNotification(message: String, isBCV: Boolean){
        val title = if (isBCV){
            "Actualización de Tasas BCV"
        } else {
            "Actualización de Tasas Monitor"
        }

        val notification = NotificationProvider().notificationBuilder(message, title).build()
        notificationManager.notify(NotificationUtils.NOTIFICATION_ID_NUMBER, notification)
    }
}