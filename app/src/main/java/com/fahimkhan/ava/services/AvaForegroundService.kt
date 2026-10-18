package com.fahimkhan.ava.services

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fahimkhan.ava.AvaApplication.Companion.CHANNEL_ID_FOREGROUND
import com.fahimkhan.ava.R
import com.fahimkhan.ava.ui.screens.MainActivity
import com.fahimkhan.ava.voice.wake.WakeWordDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AvaForegroundService : Service() {
    @Inject lateinit var wakeWordDetector: WakeWordDetector
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var alwaysListeningEnabled = false

    companion object {
        const val ACTION_START            = "com.fahimkhan.ava.START"
        const val ACTION_STOP             = "com.fahimkhan.ava.STOP"
        const val ACTION_TOGGLE_LISTENING = "com.fahimkhan.ava.TOGGLE_LISTENING"
        const val NOTIFICATION_ID         = 1001
    }

    override fun onCreate() { super.onCreate(); startForeground(NOTIFICATION_ID, buildNotification()) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // ACTION_START is what actually keeps AVA listening in the
            // background after the app UI is closed — it both subscribes
            // to wake-word detections AND starts the recognizer itself.
            // (Previously this only subscribed and never started
            // listening, so "Always Listening Mode" did nothing.)
            ACTION_START -> {
                observeWakeWords()
                alwaysListeningEnabled = true
                wakeWordDetector.startContinuousListening()
                updateNotification()
            }
            ACTION_STOP -> stopSelf()
            ACTION_TOGGLE_LISTENING -> toggleAlwaysListening()
        }
        return START_STICKY
    }

    private fun observeWakeWords() {
        scope.launch { wakeWordDetector.wakeWordDetected.collect { handleWakeWord() } }
    }

    private fun handleWakeWord() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("WAKE_WORD_TRIGGERED", true)
        }
        startActivity(intent)
    }

    private fun toggleAlwaysListening() {
        alwaysListeningEnabled = !alwaysListeningEnabled
        if (alwaysListeningEnabled) wakeWordDetector.startContinuousListening() else wakeWordDetector.stopContinuousListening()
        updateNotification()
    }

    private fun buildNotification(isListening: Boolean = false): Notification {
        val pendingInt = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle("AVA — AI Companion")
            .setContentText(if (isListening) "Always listening for AVA..." else "Neural core active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingInt)
            .setOngoing(true).setSilent(true)
            .setColor(0xFFFF1A6B.toInt())
            .build()
    }

    private fun updateNotification() = getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(alwaysListeningEnabled))

    override fun onDestroy() { scope.cancel(); wakeWordDetector.stopContinuousListening(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
