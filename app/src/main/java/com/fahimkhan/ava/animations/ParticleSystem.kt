package com.fahimkhan.ava.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fahimkhan.ava.ui.theme.AvaColors
import kotlin.math.*
import kotlin.random.Random

// ══════════════════════════════════════════════════════
//  PARTICLE DATA CLASS
// ══════════════════════════════════════════════════════

data class Particle(
    var x         : Float,
    var y         : Float,
    val vx        : Float,
    val vy        : Float,
    val radius    : Float,
    val color     : Color,
    val maxAlpha  : Float,
    var life      : Float = 1f,
    val decayRate : Float = Random.nextFloat() * 0.003f + 0.001f,
    val phaseOffset: Float = Random.nextFloat() * 2 * PI.toFloat()
)

// ══════════════════════════════════════════════════════
//  PARTICLE SYSTEM COMPONENT
// ══════════════════════════════════════════════════════

@Composable
fun FloatingParticles(
    modifier    : Modifier = Modifier,
    count       : Int      = 40,
    colors      : List<Color> = listOf(
        AvaColors.NeonPink.copy(alpha = 0.6f),
        AvaColors.NeonPurple.copy(alpha = 0.4f),
        AvaColors.CyberCyan.copy(alpha = 0.3f),
        AvaColors.NeonRed.copy(alpha = 0.3f)
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1000f,
        animationSpec = infiniteRepeatable(tween(100_000, easing = LinearEasing)),
        label         = "time"
    )

    // Generate stable particle set
    val particles = remember {
        (0 until count).map { i ->
            Particle(
                x          = Random.nextFloat(),
                y          = Random.nextFloat(),
                vx         = (Random.nextFloat() - 0.5f) * 0.0002f,
                vy         = (Random.nextFloat() - 0.5f) * 0.0002f,
                radius     = Random.nextFloat() * 3f + 1f,
                color      = colors[i % colors.size],
                maxAlpha   = Random.nextFloat() * 0.5f + 0.1f,
                phaseOffset = Random.nextFloat() * 2 * PI.toFloat()
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Drift calculation
            val drift = time * 0.001f
            val px    = ((p.x + cos(drift + p.phaseOffset) * 0.05f).mod(1f)) * w
            val py    = ((p.y + sin(drift * 0.7f + p.phaseOffset) * 0.05f).mod(1f)) * h

            // Pulse alpha
            val alpha = p.maxAlpha * ((sin(drift * 3f + p.phaseOffset) + 1f) / 2f * 0.6f + 0.4f)

            drawCircle(
                color  = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = p.radius,
                center = Offset(px, py)
            )
        }
    }
}

// ══════════════════════════════════════════════════════
//  NEON GRID BACKGROUND
// ══════════════════════════════════════════════════════

@Composable
fun NeonGrid(
    modifier   : Modifier = Modifier,
    color      : Color = AvaColors.NeonPink,
    alpha      : Float = 0.04f,
    gridSizeDp : Float = 48f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val pulse by infiniteTransition.animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gridPulse"
    )

    Canvas(modifier = modifier) {
        val gridSize = gridSizeDp.dp.toPx()
        val lineColor = color.copy(alpha = alpha * pulse)

        var x = 0f
        while (x <= size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
            x += gridSize
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
            y += gridSize
        }
    }
}

// ══════════════════════════════════════════════════════
//  SCANNING BEAM ANIMATION
// ══════════════════════════════════════════════════════

@Composable
fun ScanBeam(
    modifier : Modifier = Modifier,
    color    : Color = AvaColors.CyberCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beam")
    val progress by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "beamProgress"
    )

    Canvas(modifier = modifier) {
        val y        = size.height * progress
        val gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.1f),
                color.copy(alpha = 0.7f),
                color.copy(alpha = 0.1f),
                Color.Transparent
            )
        )
        drawRect(
            brush   = gradient,
            topLeft = Offset(0f, y - 4f),
            size    = androidx.compose.ui.geometry.Size(size.width, 8f)
        )
    }
}
