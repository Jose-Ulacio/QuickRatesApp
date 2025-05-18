package com.example.quickrates.utils.notifications

import androidx.core.app.NotificationCompat
import com.example.quickrates.App
import com.example.quickrates.R

class NotificationProvider() {
    fun notificationBuilder(message: String, title: String): NotificationCompat.Builder{
        return NotificationCompat.Builder(App.instance, NotificationUtils.NOTIFICATION_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.baseline_currency_exchange_24)
            .setPriority(NotificationUtils.NOTIFICATION_PRIORITY)
    }
}