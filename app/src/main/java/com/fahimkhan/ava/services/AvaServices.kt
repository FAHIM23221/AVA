package com.fahimkhan.ava.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets AVA actually DO things on screen — not just open an app and stop,
 * but tap the button a human would tap next (e.g. YouTube's first search
 * result). Android deliberately has no API for one app to drive another
 * app's UI except through an Accessibility Service the person has
 * explicitly turned on in Settings → Accessibility, so this only works
 * once "AVA" is enabled there. Used sparingly, for well-understood
 * single-target apps (like YouTube), never as a general "tap anything".
 */
@AndroidEntryPoint
class AvaAccessibilityService : AccessibilityService() {
    companion object {
        private val _activeAppPackage = MutableStateFlow("")
        val activeAppPackage = _activeAppPackage.asStateFlow()
        private val _screenText = MutableStateFlow("")
        val screenText = _screenText.asStateFlow()

        // The OS creates/destroys this service itself, so other classes
        // reach the running instance through this reference.
        private var instance: AvaAccessibilityService? = null
        fun isConnected(): Boolean = instance != null

        /** Taps the first clickable node whose text/description contains [textFragment]. */
        fun tapNodeContaining(textFragment: String): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val target = findClickableNode(root, textFragment.lowercase())
            return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }

        /** Taps the first clickable element below the top [yFraction] of the screen
         * (e.g. the first YouTube result thumbnail, which has no readable text). */
        fun tapFirstClickableBelow(yFraction: Float = 0.2f): Boolean {
            val root = instance?.rootInActiveWindow ?: return false
            val screenHeight = instance?.resources?.displayMetrics?.heightPixels ?: return false
            val target = findFirstClickableBelow(root, (screenHeight * yFraction).toInt())
            return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        }

        private fun findClickableNode(node: AccessibilityNodeInfo, textFragment: String): AccessibilityNodeInfo? {
            val nodeText = (node.text?.toString() ?: "").lowercase()
            val nodeDesc = (node.contentDescription?.toString() ?: "").lowercase()
            if (node.isClickable && (nodeText.contains(textFragment) || nodeDesc.contains(textFragment))) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findClickableNode(child, textFragment)?.let { return it }
            }
            return null
        }

        private fun findFirstClickableBelow(node: AccessibilityNodeInfo, minY: Int): AccessibilityNodeInfo? {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            if (node.isClickable && bounds.top >= minY && bounds.width() > 0 && bounds.height() > 0) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findFirstClickableBelow(child, minY)?.let { return it }
            }
            return null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) _activeAppPackage.value = event.packageName?.toString() ?: ""
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
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
