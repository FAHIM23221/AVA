package com.fahimkhan.ava.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahimkhan.ava.ui.components.AvaOrbComponent
import com.fahimkhan.ava.ui.components.NeonText
import com.fahimkhan.ava.ui.components.OrbState
import com.fahimkhan.ava.ui.theme.AvaColors
import kotlinx.coroutines.delay

/**
 * AVA Cinematic Splash Screen
 * Developed by Fahim Khan
 */
@Composable
fun SplashScreen(onComplete: () -> Unit) {

    var phase by remember { mutableStateOf(0) }

    val orbAlpha    by animateFloatAsState(if (phase >= 1) 1f else 0f,  animationSpec = tween(1200), label = "orbAlpha")
    val orbScale    by animateFloatAsState(if (phase >= 1) 1f else 0.3f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "orbScale")
    val titleAlpha  by animateFloatAsState(if (phase >= 2) 1f else 0f,  animationSpec = tween(800),  label = "titleAlpha")
    val titleOffset by animateFloatAsState(if (phase >= 2) 0f else 40f, animationSpec = tween(800),  label = "titleOffset")
    val subAlpha    by animateFloatAsState(if (phase >= 3) 1f else 0f,  animationSpec = tween(600),  label = "subAlpha")
    val devAlpha    by animateFloatAsState(if (phase >= 4) 1f else 0f,  animationSpec = tween(600),  label = "devAlpha")
    val exitAlpha   by animateFloatAsState(if (phase >= 5) 0f else 1f,  animationSpec = tween(800),  label = "exitAlpha")

    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        delay(200);  phase = 1
        delay(1000); phase = 2
        delay(800);  phase = 3
        delay(700);  phase = 4
        delay(1200); phase = 5
        delay(900);  onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AvaColors.BlackAbyss)
            .alpha(exitAlpha),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(500.dp)
                .alpha(glowAlpha * 0.4f)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AvaColors.NeonRed.copy(alpha = 0.6f),
                            AvaColors.NeonPurple.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.scale(orbScale).alpha(orbAlpha)) {
                AvaOrbComponent(size = 160.dp, orbState = OrbState.IDLE, showRings = false)
            }

            Spacer(Modifier.height(40.dp))

            Box(modifier = Modifier.alpha(titleAlpha).offset(y = titleOffset.dp)) {
                NeonText(
                    text = "AVA", fontSize = 72.sp, fontWeight = FontWeight.Black,
                    color = AvaColors.TextPrimary, glowColor = AvaColors.NeonPink, letterSpacing = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "AI COMPANION", fontSize = 14.sp, fontWeight = FontWeight.Light,
                color = AvaColors.TextMuted, letterSpacing = 8.sp, modifier = Modifier.alpha(subAlpha)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "v1.0.0  //  NEURAL CORE ACTIVE", fontSize = 10.sp,
                color = AvaColors.NeonPink.copy(alpha = 0.7f), letterSpacing = 3.sp,
                fontWeight = FontWeight.Light, modifier = Modifier.alpha(subAlpha)
            )

            Spacer(Modifier.height(80.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(devAlpha)) {
                Text("DEVELOPED BY", fontSize = 10.sp, color = AvaColors.TextMuted, letterSpacing = 4.sp)
                Spacer(Modifier.height(4.dp))
                NeonText(
                    text = "FAHIM KHAN", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = AvaColors.TextPrimary, glowColor = AvaColors.NeonPink, letterSpacing = 4.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .alpha(subAlpha)
        ) {
            Text(
                text = "INITIALIZING NEURAL INTERFACE...", fontSize = 9.sp,
                color = AvaColors.Active.copy(alpha = glowAlpha), letterSpacing = 2.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
