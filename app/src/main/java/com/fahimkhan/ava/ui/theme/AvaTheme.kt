package com.fahimkhan.ava.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AvaColors {
    val DeepBlack       = Color(0xFF000000)
    val BlackAbyss      = Color(0xFF050508)
    val NightVoid       = Color(0xFF0A0A0F)
    val DarkSurface     = Color(0xFF0D0D15)
    val CardSurface     = Color(0xFF12121A)
    val ElevatedSurface = Color(0xFF1A1A25)
    val NeonRed         = Color(0xFFFF0033)
    val CrimsonGlow     = Color(0xFFE8003A)
    val HotPink         = Color(0xFFFF1A6B)
    val NeonPink        = Color(0xFFFF2D78)
    val DeepRose        = Color(0xFFCC0044)
    val BlushGlow       = Color(0xFFFF6B9D)
    val NeonPurple      = Color(0xFF8B00FF)
    val ElectricViolet  = Color(0xFF6600CC)
    val CyberPurple     = Color(0xFFAA44FF)
    val CyberCyan       = Color(0xFF00FFFF)
    val IceCyan         = Color(0xFF44EEFF)
    val TextPrimary     = Color(0xFFF0F0F8)
    val TextSecondary   = Color(0xFFAAAAAA)
    val TextMuted       = Color(0xFF666677)
    val TextGlow        = Color(0xFFFFCCDD)
    val Active          = Color(0xFF00FF88)
    val Warning         = Color(0xFFFFAA00)
    val Error           = NeonRed
    val Inactive        = Color(0xFF333344)
    val OrbCore         = Color(0xFFFF1A5E)
    val OrbMid          = Color(0xFF8B00FF)
    val OrbOuter        = Color(0xFF200010)
    val OrbListening    = Color(0xFF00BFFF)
    val OrbThinking     = Color(0xFFFFAA00)
    val OrbSpeaking     = NeonPink
    val OrbIdle         = CrimsonGlow
}

private val AvaDarkColorScheme = darkColorScheme(
    primary           = AvaColors.NeonPink,
    onPrimary         = AvaColors.DeepBlack,
    primaryContainer  = AvaColors.DeepRose,
    secondary         = AvaColors.CyberPurple,
    onSecondary       = AvaColors.DeepBlack,
    background        = AvaColors.BlackAbyss,
    onBackground      = AvaColors.TextPrimary,
    surface           = AvaColors.DarkSurface,
    onSurface         = AvaColors.TextPrimary,
    surfaceVariant    = AvaColors.CardSurface,
    error             = AvaColors.NeonRed,
    outline           = AvaColors.NeonPink.copy(alpha = 0.4f),
    outlineVariant    = AvaColors.ElevatedSurface,
)

data class AvaTypography(
    val displayLarge  : TextStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black,     fontSize = 48.sp, letterSpacing = 4.sp,  color = AvaColors.TextPrimary),
    val headlineLarge : TextStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,      fontSize = 24.sp, letterSpacing = 2.sp,  color = AvaColors.NeonPink),
    val bodyLarge     : TextStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,    fontSize = 16.sp, letterSpacing = 0.5.sp, color = AvaColors.TextSecondary),
    val labelSmall    : TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,  fontSize = 11.sp, letterSpacing = 1.5.sp, color = AvaColors.TextMuted)
)

val LocalAvaTypography = staticCompositionLocalOf { AvaTypography() }

@Composable
fun AvaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAvaTypography provides AvaTypography()) {
        MaterialTheme(colorScheme = AvaDarkColorScheme, content = content)
    }
}

object AvaTheme {
    val colors get() = AvaColors
    val typography @Composable get() = LocalAvaTypography.current
}
