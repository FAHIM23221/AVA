package com.fahimkhan.ava.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahimkhan.ava.ui.theme.AvaColors
import kotlin.math.*

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING, SLEEPING }

/**
 * AVA's living animated AI orb — the visual soul of the app.
 */
@Composable
fun AvaOrbComponent(
    modifier   : Modifier  = Modifier,
    size       : Dp        = 220.dp,
    orbState   : OrbState  = OrbState.IDLE,
    audioLevel : Float     = 0f,
    showRings  : Boolean   = true
) {
    val targetCoreColor = when (orbState) {
        OrbState.IDLE      -> AvaColors.OrbIdle
        OrbState.LISTENING -> AvaColors.OrbListening
        OrbState.THINKING  -> AvaColors.OrbThinking
        OrbState.SPEAKING  -> AvaColors.OrbSpeaking
        OrbState.SLEEPING  -> AvaColors.Inactive
    }
    val coreColor by animateColorAsState(targetCoreColor, tween(600), label = "coreColor")

    val infiniteTransition = rememberInfiniteTransition(label = "orbAnim")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val ring1Rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ring1"
    )
    val ring2Rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ring2"
    )
    val ring3Rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "ring3"
    )
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "particlePhase"
    )
    val thinkingPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "thinkPulse"
    )

    val audioScale = if (orbState == OrbState.LISTENING) 1f + audioLevel * 0.3f else 1f
    val effectiveScale = breathScale * audioScale

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.6f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val orbRadius = (size.toPx() / 2f) * effectiveScale

            // Outer ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor.copy(alpha = 0.05f * glowAlpha), Color.Transparent),
                    center = center, radius = orbRadius * 2.8f
                ),
                radius = orbRadius * 2.8f, center = center
            )

            // Mid glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.2f * glowAlpha),
                        AvaColors.NeonPurple.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center, radius = orbRadius * 1.8f
                ),
                radius = orbRadius * 1.8f, center = center
            )

            // Rotating plasma rings
            if (showRings && orbState != OrbState.SLEEPING) {
                drawPlasmaRing(center, orbRadius * 1.4f, ring1Rotation, coreColor.copy(alpha = 0.4f * glowAlpha), 1.5f)
                drawPlasmaRing(center, orbRadius * 1.6f, ring2Rotation, AvaColors.NeonPurple.copy(alpha = 0.25f * glowAlpha), 1f, dashed = true)
                if (orbState != OrbState.IDLE) {
                    drawPlasmaRing(center, orbRadius * 1.2f, ring3Rotation, coreColor.copy(alpha = 0.5f), 2f)
                }
            }

            // Core orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreColor.copy(alpha = 0.8f),
                        coreColor.copy(alpha = 0.4f),
                        AvaColors.NeonPurple.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - orbRadius * 0.2f, center.y - orbRadius * 0.25f),
                    radius = orbRadius
                ),
                radius = orbRadius, center = center
            )

            // Core shine highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(center.x - orbRadius * 0.3f, center.y - orbRadius * 0.35f),
                    radius = orbRadius * 0.35f
                ),
                radius = orbRadius * 0.35f,
                center = Offset(center.x - orbRadius * 0.3f, center.y - orbRadius * 0.35f)
            )

            // Thinking dots
            if (orbState == OrbState.THINKING) {
                for (i in 0..5) {
                    val angle = thinkingPulse + (i * PI.toFloat() / 3f)
                    val dotX = center.x + cos(angle) * orbRadius * 1.15f
                    val dotY = center.y + sin(angle) * orbRadius * 1.15f
                    val dotAlpha = (sin(thinkingPulse + i * 0.8f) + 1f) / 2f
                    drawCircle(
                        color = AvaColors.OrbThinking.copy(alpha = dotAlpha * 0.9f),
                        radius = 4.dp.toPx(), center = Offset(dotX, dotY)
                    )
                }
            }

            // Listening wave rings
            if (orbState == OrbState.LISTENING) {
                for (i in 1..3) {
                    val waveRadius = orbRadius * (1.2f + i * 0.25f + sin(particlePhase + i) * 0.05f)
                    drawCircle(
                        color = AvaColors.OrbListening.copy(alpha = (0.3f / i) * glowAlpha),
                        radius = waveRadius, center = center,
                        style = Stroke(width = (3f / i).dp.toPx())
                    )
                }
            }

            // Speaking particles
            if (orbState == OrbState.SPEAKING) {
                for (i in 0..11) {
                    val angle = particlePhase * 2f + (i * 2 * PI.toFloat() / 12f)
                    val radiusMul = 1.3f + sin(particlePhase * 3f + i) * 0.15f
                    val px = center.x + cos(angle) * orbRadius * radiusMul
                    val py = center.y + sin(angle) * orbRadius * radiusMul
                    val pAlpha = (sin(particlePhase + i * 0.5f) + 1f) / 2f * 0.8f
                    drawCircle(
                        color = AvaColors.NeonPink.copy(alpha = pAlpha),
                        radius = 3.dp.toPx(), center = Offset(px, py)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPlasmaRing(
    center: Offset, radius: Float, rotation: Float,
    color: Color, strokeWidth: Float, dashed: Boolean = false
) {
    rotate(rotation, center) {
        if (dashed) {
            for (i in 0..7) {
                drawArc(
                    color = color, startAngle = i * 45f, sweepAngle = 25f, useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        } else {
            drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth.dp.toPx()))
        }
    }
}
