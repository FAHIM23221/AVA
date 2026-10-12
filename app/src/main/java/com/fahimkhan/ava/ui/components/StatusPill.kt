package com.fahimkhan.ava.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahimkhan.ava.ui.theme.AvaColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun StatusPill(
    text     : String,
    isActive : Boolean = true,
    modifier : Modifier = Modifier
) {
    val color = if (isActive) AvaColors.Active else AvaColors.TextMuted
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp))
        )
        Text(text = text, color = color, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun VoiceWaveVisualizer(
    isActive   : Boolean = false,
    audioLevel : Float = 0f,
    barCount   : Int = 32,
    modifier   : Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val barWidth  = (size.width / barCount) * 0.6f
        val gap       = (size.width / barCount) * 0.4f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + gap / 2
            val normalizedPos = i.toFloat() / barCount
            val wave     = sin(wavePhase + normalizedPos * 4 * PI).toFloat()
            val envelope = sin(normalizedPos * PI).toFloat()

            val heightFactor = if (isActive) {
                (0.15f + 0.85f * abs(wave) * envelope * (0.3f + audioLevel * 0.7f))
            } else {
                0.08f + abs(wave) * 0.05f
            }

            val barHeight = maxHeight * heightFactor.coerceIn(0.05f, 1f)
            val top = (maxHeight - barHeight) / 2f
            val alpha = if (isActive) 0.6f + abs(wave) * 0.4f else 0.25f

            drawRoundRect(
                color = AvaColors.NeonPink.copy(alpha = alpha),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
