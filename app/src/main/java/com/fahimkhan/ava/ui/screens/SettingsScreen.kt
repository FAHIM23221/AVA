package com.fahimkhan.ava.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahimkhan.ava.ui.components.CyberCard
import com.fahimkhan.ava.ui.components.NeonText
import com.fahimkhan.ava.ui.theme.AvaColors

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var alwaysListening  by remember { mutableStateOf(false) }
    var bengaliEnabled   by remember { mutableStateOf(true) }
    var emotionalMode    by remember { mutableStateOf(true) }
    var elevenLabsVoice  by remember { mutableStateOf(false) }
    var darkOverlay      by remember { mutableStateOf(false) }
    var hapticFeedback   by remember { mutableStateOf(true) }
    var selectedMode     by remember { mutableStateOf(0) }
    val modes = listOf("ASSISTANT", "GF MODE", "PROFESSIONAL")

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).systemBarsPadding()
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = AvaColors.NeonPink) }
                Spacer(Modifier.width(8.dp))
                Column {
                    NeonText("SETTINGS", fontSize = 20.sp, glowColor = AvaColors.NeonPink, color = AvaColors.TextPrimary, letterSpacing = 4.sp)
                    Text("SYSTEM CONFIGURATION", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }

            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Personality Mode
                SettingsSection("PERSONALITY MODE", Icons.Rounded.Psychology, AvaColors.CyberPurple) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEachIndexed { i, mode ->
                            val active = i == selectedMode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (active) AvaColors.CyberPurple.copy(0.2f) else AvaColors.DarkSurface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp,
                                        if (active) AvaColors.CyberPurple else AvaColors.ElevatedSurface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedMode = i }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            ) {
                                Text(mode, color = if (active) AvaColors.CyberPurple else AvaColors.TextMuted,
                                    fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Voice settings
                SettingsSection("VOICE & LANGUAGE", Icons.Rounded.RecordVoiceOver, AvaColors.NeonPink) {
                    SettingsToggle("Always Listening Mode", "Wake on 'AVA'", alwaysListening) { alwaysListening = it }
                    SettingsToggle("Bengali Language Support", "বাংলায় কথা বলুন", bengaliEnabled) { bengaliEnabled = it }
                    SettingsToggle("ElevenLabs Premium Voice", "Natural AI voice (API key required)", elevenLabsVoice) { elevenLabsVoice = it }
                }

                // AI behaviour
                SettingsSection("AI BEHAVIOUR", Icons.Rounded.AutoAwesome, AvaColors.OrbThinking) {
                    SettingsToggle("Emotional Mode", "AVA reacts with emotion", emotionalMode) { emotionalMode = it }
                    SettingsToggle("Haptic Feedback", "Vibrate on interactions", hapticFeedback) { hapticFeedback = it }
                }

                // Display
                SettingsSection("DISPLAY", Icons.Rounded.Palette, AvaColors.IceCyan) {
                    SettingsToggle("System Overlay", "Float AVA over other apps", darkOverlay) { darkOverlay = it }
                }

                // User profile
                SettingsSection("USER PROFILE", Icons.Rounded.Person, AvaColors.Active) {
                    var userName by remember { mutableStateOf("") }
                    Text("Your name (AVA will address you)", color = AvaColors.TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = userName,
                        onValueChange = { userName = it },
                        placeholder   = { Text("Enter your name", color = AvaColors.TextMuted) },
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AvaColors.Active,
                            unfocusedBorderColor = AvaColors.Active.copy(0.3f),
                            focusedTextColor     = AvaColors.TextPrimary,
                            unfocusedTextColor   = AvaColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // About
                SettingsSection("ABOUT", Icons.Rounded.Info, AvaColors.TextMuted) {
                    SettingsInfoRow("App",       "AVA — AI Companion")
                    SettingsInfoRow("Version",   "1.0.0")
                    SettingsInfoRow("Developer", "Fahim Khan")
                    SettingsInfoRow("Build",     "Neural Core — Production")
                    Spacer(Modifier.height(8.dp))
                    Divider(color = AvaColors.ElevatedSurface)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        NeonText("DEVELOPED BY FAHIM KHAN", fontSize = 12.sp, glowColor = AvaColors.NeonPink,
                            color = AvaColors.TextPrimary, letterSpacing = 3.sp)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title   : String,
    icon    : ImageVector,
    color   : androidx.compose.ui.graphics.Color,
    content : @Composable ColumnScope.() -> Unit
) {
    CyberCard(glowColor = color.copy(0.4f)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Divider(color = color.copy(0.2f))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsToggle(label: String, sub: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, color = AvaColors.TextPrimary, fontSize = 13.sp)
            Text(sub,   color = AvaColors.TextMuted,   fontSize = 10.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = AvaColors.TextPrimary,
                checkedTrackColor   = AvaColors.NeonPink,
                uncheckedTrackColor = AvaColors.CardSurface
            )
        )
    }
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AvaColors.TextMuted,     fontSize = 12.sp)
        Text(value, color = AvaColors.TextSecondary, fontSize = 12.sp)
    }
}
