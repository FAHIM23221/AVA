package com.fahimkhan.ava

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AvaApplication : Application() {
    companion object {
        const val CHANNEL_ID_FOREGROUND = "ava_foreground"
        const val CHANNEL_ID_ALERTS     = "ava_alerts"
        const val CHANNEL_ID_SILENT     = "ava_silent"
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID_FOREGROUND, "AVA — Always On", NotificationManager.IMPORTANCE_LOW).apply { description = "AVA is active"; setShowBadge(false) })
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID_ALERTS, "AVA Alerts", NotificationManager.IMPORTANCE_HIGH).apply { description = "Security and important alerts" })
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID_SILENT, "AVA Background", NotificationManager.IMPORTANCE_MIN).apply { description = "Silent background operations"; setShowBadge(false) })
        }
    }
}
