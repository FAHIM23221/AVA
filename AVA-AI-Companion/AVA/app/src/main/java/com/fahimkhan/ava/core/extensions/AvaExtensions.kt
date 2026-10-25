package com.fahimkhan.ava.core.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.fahimkhan.ava.services.AvaForegroundService
import java.text.SimpleDateFormat
import java.util.*

// ── Context Extensions ──────────────────────────────────

fun Context.startAvaService() {
    val intent = Intent(this, AvaForegroundService::class.java).apply {
        action = AvaForegroundService.ACTION_START
    }
    startForegroundService(intent)
}

fun Context.stopAvaService() {
    val intent = Intent(this, AvaForegroundService::class.java).apply {
        action = AvaForegroundService.ACTION_STOP
    }
    startService(intent)
}

fun Context.hasOverlayPermission(): Boolean =
    Settings.canDrawOverlays(this)

fun Context.requestOverlayPermission() {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}

fun Context.vibrate(pattern: LongArray = longArrayOf(0, 50, 50, 100)) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
    } else {
        @Suppress("DEPRECATION")
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        v.vibrate(pattern, -1)
    }
}

fun Context.shortToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

// ── String Extensions ───────────────────────────────────

fun String.isBengali(): Boolean {
    return this.any { it.code in 0x0980..0x09FF }
}

fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else "${take(maxLength)}..."

fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

// ── Date Extensions ─────────────────────────────────────

fun Long.toFormattedTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))

fun Long.toFormattedDateTime(): String =
    SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()).format(Date(this))

fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000         -> "Just now"
        diff < 3_600_000      -> "${diff / 60_000}m ago"
        diff < 86_400_000     -> "${diff / 3_600_000}h ago"
        else                  -> "${diff / 86_400_000}d ago"
    }
}

// ── Color Extensions ────────────────────────────────────

fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

// ── Number Extensions ───────────────────────────────────

fun Float.toPercent(): String = "${(this * 100).toInt()}%"
fun Int.toPaddedString(length: Int = 2): String = toString().padStart(length, '0')
