package com.fahimkhan.ava.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fahimkhan.ava.ui.components.*
import com.fahimkhan.ava.ui.theme.AvaColors

/**
 * ADMIN PANEL — Secured control center
 * Access: 5 taps on top bar
 * Developed by Fahim Khan
 */
@Composable
fun AdminScreen(
    viewModel : AdminViewModel = hiltViewModel(),
    onBack    : () -> Unit
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val adminState       by viewModel.adminState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {
        AdminGridBackground()

        if (!isAuthenticated) {
            AdminLoginPanel(onLogin = viewModel::authenticate, onBack = onBack)
        } else {
            AdminDashboard(state = adminState, viewModel = viewModel, onBack = onBack)
        }
    }
}

@Composable
fun AdminLoginPanel(onLogin: (String) -> Unit, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(AvaColors.NeonRed.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .border(1.dp, AvaColors.NeonRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Icon(Icons.Rounded.AdminPanelSettings, null, tint = AvaColors.NeonRed, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(24.dp))
        NeonText("ADMIN ACCESS", fontSize = 22.sp, glowColor = AvaColors.NeonRed, color = AvaColors.TextPrimary, letterSpacing = 4.sp)
        Spacer(Modifier.height(8.dp))
        Text("RESTRICTED ZONE — AUTHORIZED PERSONNEL ONLY", color = AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = false },
            label = { Text("ADMIN PASSWORD", color = AvaColors.TextMuted, fontSize = 11.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AvaColors.NeonRed,
                unfocusedBorderColor = AvaColors.NeonRed.copy(alpha = 0.4f),
                cursorColor = AvaColors.NeonRed,
                focusedTextColor = AvaColors.TextPrimary,
                unfocusedTextColor = AvaColors.TextPrimary,
                errorBorderColor = AvaColors.Error
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ACCESS DENIED — INVALID CREDENTIALS", color = AvaColors.NeonRed, fontSize = 10.sp, letterSpacing = 2.sp)
        }

        Spacer(Modifier.height(24.dp))

        NeonButton(text = "AUTHENTICATE", color = AvaColors.NeonRed, onClick = { onLogin(password) }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("← RETURN", color = AvaColors.TextMuted, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun AdminDashboard(state: AdminState, viewModel: AdminViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).systemBarsPadding()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = AvaColors.NeonRed) }
            Spacer(Modifier.width(8.dp))
            Column {
                NeonText("ADMIN CONTROL", fontSize = 20.sp, glowColor = AvaColors.NeonRed, color = AvaColors.TextPrimary, letterSpacing = 3.sp)
                Text("AVA NEURAL COMMAND CENTER", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.weight(1f))
            StatusPill("ROOT ACCESS", isActive = true)
        }

        Spacer(Modifier.height(24.dp))

        AdminCard(title = "SYSTEM STATUS", icon = Icons.Rounded.Memory, glowColor = AvaColors.Active) {
            AdminStatRow("Neural Core",    state.neuralCoreStatus)
            AdminStatRow("Voice Engine",   state.voiceEngineStatus)
            AdminStatRow("Memory System",  state.memoryStatus)
            AdminStatRow("Security Layer", state.securityStatus)
            AdminStatRow("Vision Module",  state.visionStatus)
            AdminStatRow("Wake Word",      state.wakeWordStatus)
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "AI BRAIN CONFIG", icon = Icons.Rounded.Psychology, glowColor = AvaColors.NeonPink) {
            AdminToggleRow("Gemini AI",        state.geminiEnabled,     viewModel::toggleGemini)
            AdminToggleRow("ElevenLabs Voice", state.elevenLabsEnabled, viewModel::toggleElevenLabs)
            AdminToggleRow("Bengali Language", state.bengaliEnabled,    viewModel::toggleBengali)
            AdminToggleRow("Emotional Mode",   state.emotionalMode,     viewModel::toggleEmotionalMode)
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "PERSONALITY CONFIG", icon = Icons.Rounded.Face, glowColor = AvaColors.CyberPurple) {
            AdminToggleRow("GF Mode",           state.gfModeEnabled,    viewModel::toggleGfMode)
            AdminToggleRow("Professional Mode", state.professionalMode, viewModel::toggleProfessionalMode)
            Text(
                "CALL STYLE: ${if (state.gfModeEnabled) "Emotional/Intimate" else "Boss/Professional"}",
                color = AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "SECURITY SYSTEM", icon = Icons.Rounded.Security, glowColor = AvaColors.NeonRed) {
            AdminToggleRow("Owner Recognition",  state.ownerRecognitionOn, viewModel::toggleOwnerRecognition)
            AdminToggleRow("Intruder Detection", state.intruderDetection,  viewModel::toggleIntruderDetection)
            AdminToggleRow("Security Logging",   state.securityLogging,    viewModel::toggleSecurityLogging)
            AdminToggleRow("Auto Lock",          state.autoLock,           viewModel::toggleAutoLock)
            Spacer(Modifier.height(8.dp))
            NeonButton("VIEW SECURITY LOGS", AvaColors.NeonRed, {}, Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "ALWAYS-ON SYSTEM", icon = Icons.Rounded.Hearing, glowColor = AvaColors.OrbListening) {
            AdminToggleRow("Always Listening",   state.alwaysListening, viewModel::toggleAlwaysListening)
            AdminToggleRow("Wake Word Active",   state.wakeWordActive,  viewModel::toggleWakeWord)
            AdminToggleRow("Auto Start on Boot", state.autoStartBoot,   viewModel::toggleAutoStart)
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "MEMORY SYSTEM", icon = Icons.Rounded.DataObject, glowColor = AvaColors.IceCyan) {
            AdminStatRow("Short-term Memories",  state.shortTermCount.toString())
            AdminStatRow("Long-term Memories",   state.longTermCount.toString())
            AdminStatRow("Face Profiles",        state.faceProfileCount.toString())
            AdminStatRow("Conversations Logged", state.conversationCount.toString())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("EXPORT", AvaColors.IceCyan, {}, Modifier.weight(1f))
                NeonButton("CLEAR ALL", AvaColors.NeonRed, viewModel::clearAllMemory, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "API CONFIGURATION", icon = Icons.Rounded.Key, glowColor = AvaColors.Warning) {
            Text("Gemini API Key", color = AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(state.geminiKeyMasked, color = AvaColors.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text("ElevenLabs API Key", color = AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(state.elevenLabsKeyMasked, color = AvaColors.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            NeonButton("UPDATE API KEYS", AvaColors.Warning, {}, Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))

        AdminCard(title = "ABOUT AVA", icon = Icons.Rounded.Info, glowColor = AvaColors.NeonPurple) {
            Text("AVA — AI Companion", color = AvaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0  //  Neural Core Build", color = AvaColors.TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Divider(color = AvaColors.NeonPurple.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))
            Text("DEVELOPED BY", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            NeonText("FAHIM KHAN", fontSize = 18.sp, glowColor = AvaColors.NeonPink, color = AvaColors.TextPrimary, letterSpacing = 4.sp)
            Spacer(Modifier.height(8.dp))
            Text("All rights reserved. Unauthorized access prohibited.", color = AvaColors.TextMuted, fontSize = 9.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AdminCard(
    title     : String,
    icon      : androidx.compose.ui.graphics.vector.ImageVector,
    glowColor : Color = AvaColors.NeonPink,
    content   : @Composable ColumnScope.() -> Unit
) {
    CyberCard(glowColor = glowColor) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = glowColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = glowColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Divider(color = glowColor.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun AdminStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AvaColors.TextMuted, fontSize = 12.sp)
        Text(value, color = AvaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AdminToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AvaColors.TextSecondary, fontSize = 12.sp)
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AvaColors.TextPrimary,
                checkedTrackColor = AvaColors.NeonPink,
                uncheckedTrackColor = AvaColors.CardSurface
            )
        )
    }
}

@Composable
fun AdminGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40.dp.toPx()
        val color = AvaColors.NeonRed.copy(alpha = 0.03f)
        var x = 0f
        while (x <= size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height))
            x += gridSize
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y))
            y += gridSize
        }
    }
}
