package com.fahimkhan.ava.core.utils

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceControlSystem @Inject constructor(@ApplicationContext private val context: Context) {
    private val audioManager  = context.getSystemService(Context.AUDIO_SERVICE)  as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val wifiManager   = context.getSystemService(Context.WIFI_SERVICE)   as WifiManager
    private var torchOn = false

    fun setVolume(level: Int) { val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (max * level / 100f).toInt().coerceIn(0, max), 0) }
    fun increaseVolume() = audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
    fun decreaseVolume() = audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
    fun muteVolume()     = audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,  0)

    fun toggleFlashlight(): Boolean {
        return try {
            val id = cameraManager.cameraIdList.firstOrNull() ?: return false
            torchOn = !torchOn
            cameraManager.setTorchMode(id, torchOn)
            torchOn
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun setWifi(enabled: Boolean) { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) wifiManager.isWifiEnabled = enabled }
    fun isWifiEnabled() = wifiManager.isWifiEnabled

    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        val cmd = command.lowercase()
        when {
            cmd.contains("flashlight") || cmd.contains("torch") -> { val on = toggleFlashlight(); "Flashlight ${if (on) "on" else "off"}, Boss." }
            cmd.contains("volume up")   -> { increaseVolume(); "Volume increased, Boss." }
            cmd.contains("volume down") -> { decreaseVolume(); "Volume decreased, Boss." }
            cmd.contains("mute")        -> { muteVolume();     "Muted, Boss." }
            cmd.contains("wifi on")     -> { setWifi(true);    "Wi-Fi enabled, Boss." }
            cmd.contains("wifi off")    -> { setWifi(false);   "Wi-Fi disabled, Boss." }
            else -> ""
        }
    }
}
