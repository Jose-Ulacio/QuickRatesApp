package com.example.quickrates

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.quickrates.utils.notifications.NotificationUtils

class App: Application() {
    companion object{
        lateinit var instance: App

        fun getNotificationManager(): NotificationManagerCompat{
            val notificationManager = NotificationManagerCompat.from(instance)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val channel = NotificationChannel(
                    NotificationUtils.NOTIFICATION_ID,
                    NotificationUtils.NOTIFICATION_NAME,
                    NotificationUtils.NOTIFICATION_IMPORTANCE
                )
                notificationManager.createNotificationChannel(channel)
            }
            return notificationManager
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}