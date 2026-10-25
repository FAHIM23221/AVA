package com.fahimkhan.ava.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class AvaAccessibilityService : AccessibilityService() {
    companion object {
        private val _activeAppPackage = MutableStateFlow("")
        val activeAppPackage = _activeAppPackage.asStateFlow()
        private val _screenText = MutableStateFlow("")
        val screenText = _screenText.asStateFlow()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) _activeAppPackage.value = event.packageName?.toString() ?: ""
    }
    override fun onInterrupt() {}
}

@AndroidEntryPoint
class AvaNotificationListenerService : NotificationListenerService() {
    companion object {
        private val _latestNotification = MutableStateFlow<NotificationInfo?>(null)
        val latestNotification = _latestNotification.asStateFlow()
    }
    data class NotificationInfo(val appPackage: String, val title: String, val text: String, val timestamp: Long)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification.extras
        _latestNotification.value = NotificationInfo(sbn.packageName, extras.getString(android.app.Notification.EXTRA_TITLE) ?: "", extras.getString(android.app.Notification.EXTRA_TEXT) ?: "", sbn.postTime)
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}

@AndroidEntryPoint
class WakeWordService : android.app.Service() {
    @javax.inject.Inject lateinit var wakeWordDetector: com.fahimkhan.ava.voice.wake.WakeWordDetector
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { wakeWordDetector.startContinuousListening(); return START_STICKY }
    override fun onDestroy() { wakeWordDetector.stopContinuousListening(); super.onDestroy() }
    override fun onBind(intent: Intent?) = null
}

class AvaOverlayService : android.app.Service() {
    override fun onBind(intent: Intent?) = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("auto_start_boot", false)) {
                context.startForegroundService(Intent(context, AvaForegroundService::class.java).apply { action = AvaForegroundService.ACTION_START })
            }
        }
    }
}
