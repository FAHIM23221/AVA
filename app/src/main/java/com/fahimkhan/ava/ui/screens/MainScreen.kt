package com.fahimkhan.ava.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fahimkhan.ava.ui.components.*
import com.fahimkhan.ava.ui.theme.AvaColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.*

enum class AvaMood { CALM, ALERT, HAPPY, FOCUSED, PROTECTIVE }

fun getMoodLabel(mood: AvaMood) = when (mood) {
    AvaMood.CALM       -> "◈ CALM MODE"
    AvaMood.ALERT      -> "◈ ALERT MODE"
    AvaMood.HAPPY      -> "◈ HAPPY MODE"
    AvaMood.FOCUSED    -> "◈ FOCUSED MODE"
    AvaMood.PROTECTIVE -> "◈ PROTECTIVE MODE"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel          : MainViewModel = hiltViewModel(),
    onNavigateAdmin    : () -> Unit,
    onNavigateMemory   : () -> Unit,
    onNavigateSettings : () -> Unit,
    onNavigateVision   : () -> Unit,
    onNavigateSecurity : () -> Unit
) {
    val orbState   by viewModel.orbState.collectAsState()
    val subtitle   by viewModel.subtitleText.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val mood       by viewModel.avaMood.collectAsState()

    var tapCount by remember { mutableStateOf(0) }
    var lastTap  by remember { mutableStateOf(0L) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var showMicDeniedHint by remember { mutableStateOf(false) }
    var awaitingMicGrant  by remember { mutableStateOf(false) }

    LaunchedEffect(micPermission.status.isGranted) {
        if (awaitingMicGrant && micPermission.status.isGranted) {
            awaitingMicGrant = false
            showMicDeniedHint = false
            viewModel.toggleListening()
        } else if (awaitingMicGrant && !micPermission.status.isGranted) {
            // User just denied the system dialog
            awaitingMicGrant = false
            showMicDeniedHint = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {

        ParticleBackground()

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        AvaColors.NeonRed.copy(alpha = 0.04f),
                        AvaColors.NeonPurple.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    radius = 800f
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvaTopBar(
                avaMood = mood,
                onTap = {
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTap < 600) tapCount + 1 else 1
                    lastTap  = now
                    if (tapCount >= 5) { tapCount = 0; onNavigateAdmin() }
                }
            )

            Spacer(Modifier.weight(0.5f))

            Text(getMoodLabel(mood), color = AvaColors.TextMuted, fontSize = 11.sp, letterSpacing = 3.sp)

            Spacer(Modifier.height(8.dp))

            AvaOrbComponent(size = 230.dp, orbState = orbState, audioLevel = audioLevel, showRings = true)

            Spacer(Modifier.height(16.dp))

            AvaSubtitleCard(
                text = if (showMicDeniedHint) "I need microphone access to hear you, Boss. Please allow it in your phone's App Settings." else subtitle,
                orbState = orbState
            )

            Spacer(Modifier.weight(1f))

            VoiceWaveVisualizer(
                isActive = orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING,
                audioLevel = audioLevel,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            AvaInteractionPanel(
                orbState = orbState,
                onMicClick = {
                    if (micPermission.status.isGranted) {
                        showMicDeniedHint = false
                        viewModel.toggleListening()
                    } else {
                        awaitingMicGrant = true
                        micPermission.launchPermissionRequest()
                    }
                },
                onNavigateMemory = onNavigateMemory,
                onNavigateVision = onNavigateVision,
                onNavigateSecurity = onNavigateSecurity,
                onNavigateSettings = onNavigateSettings
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AvaTopBar(avaMood: AvaMood, onTap: () -> Unit) {
    val time by produceState(initialValue = "", producer = {
        while (true) {
            value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(10_000)
        }
    })

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(time, color = AvaColors.TextMuted, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NeonText("AVA", fontSize = 18.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Black,
                color = AvaColors.TextPrimary, glowColor = AvaColors.NeonPink)
            Text("AI COMPANION", color = AvaColors.TextMuted, fontSize = 8.sp, letterSpacing = 3.sp)
        }
        Spacer(Modifier.weight(1f))
        StatusPill(text = "ACTIVE", isActive = true)
    }
}

@Composable
fun AvaSubtitleCard(text: String, orbState: OrbState) {
    val alpha by animateFloatAsState(if (text.isBlank()) 0f else 1f, tween(300), label = "sub")

    Box(modifier = Modifier.alpha(alpha).fillMaxWidth().padding(horizontal = 32.dp)) {
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = when (orbState) {
                OrbState.LISTENING -> AvaColors.OrbListening
                OrbState.THINKING  -> AvaColors.OrbThinking
                else               -> AvaColors.NeonPink
            }
        ) {
            Text(
                text = text.ifBlank { "..." }, color = AvaColors.TextPrimary, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun AvaInteractionPanel(
    orbState           : OrbState,
    onMicClick         : () -> Unit,
    onNavigateMemory   : () -> Unit,
    onNavigateVision   : () -> Unit,
    onNavigateSecurity : () -> Unit,
    onNavigateSettings : () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(contentAlignment = Alignment.Center) {
            val ringScale by rememberInfiniteTransition(label = "ring").animateFloat(
                initialValue = 1f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "ringScale"
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (orbState == OrbState.LISTENING) ringScale else 1f)
                    .background(AvaColors.NeonPink.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, AvaColors.NeonPink.copy(alpha = 0.3f), CircleShape)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (orbState == OrbState.LISTENING) AvaColors.OrbListening else AvaColors.NeonPink,
                                if (orbState == OrbState.LISTENING) AvaColors.NeonPurple else AvaColors.DeepRose
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { onMicClick() }
            ) {
                Icon(
                    imageVector = if (orbState == OrbState.LISTENING) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = "Voice", tint = Color.White, modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (orbState == OrbState.LISTENING) "LISTENING..." else "TAP TO SPEAK",
            color = AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 3.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
            QuickActionButton(Icons.Rounded.Psychology, "MEMORY",   onNavigateMemory,   Modifier.weight(1f))
            QuickActionButton(Icons.Rounded.Visibility,  "VISION",   onNavigateVision,   Modifier.weight(1f))
            QuickActionButton(Icons.Rounded.Shield,      "SECURITY", onNavigateSecurity, Modifier.weight(1f))
            QuickActionButton(Icons.Rounded.Settings,    "SETTINGS", onNavigateSettings, Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickActionButton(
    icon     : androidx.compose.ui.graphics.vector.ImageVector,
    label    : String,
    onClick  : () -> Unit,
    modifier : Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .background(AvaColors.CardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, AvaColors.NeonPink.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = AvaColors.NeonPink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = AvaColors.TextMuted, fontSize = 8.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun ParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "phase")

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val particles = listOf(
            Triple(0.1f, 0.2f, 2.5f), Triple(0.85f, 0.15f, 1.5f),
            Triple(0.3f, 0.7f, 2f),   Triple(0.7f, 0.6f, 1f),
            Triple(0.5f, 0.9f, 1.8f), Triple(0.15f, 0.5f, 1.2f),
            Triple(0.9f, 0.4f, 2.2f), Triple(0.45f, 0.35f, 1.6f),
            Triple(0.6f, 0.1f, 1.3f), Triple(0.25f, 0.85f, 0.8f),
            Triple(0.75f, 0.8f, 1.9f),Triple(0.05f, 0.75f, 2.1f),
        )
        particles.forEachIndexed { idx, (xFrac, yFrac, baseRadius) ->
            val drift = kotlin.math.sin((phase + idx * 0.3f) * 2 * kotlin.math.PI).toFloat()
            val px = xFrac * size.width + drift * 15f
            val py = yFrac * size.height + kotlin.math.cos((phase + idx * 0.5f) * 2 * kotlin.math.PI).toFloat() * 12f
            val alpha = (kotlin.math.sin((phase + idx * 0.7f) * 2 * kotlin.math.PI).toFloat() + 1f) / 2f * 0.35f + 0.05f
            drawCircle(AvaColors.NeonPink.copy(alpha = alpha), radius = baseRadius.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
        }
    }
}
