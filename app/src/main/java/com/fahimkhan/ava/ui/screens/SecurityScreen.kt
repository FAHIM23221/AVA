package com.fahimkhan.ava.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ai.memory.SecurityLog
import com.fahimkhan.ava.security.owner.OwnerSecuritySystem
import com.fahimkhan.ava.security.owner.UserTrustLevel
import com.fahimkhan.ava.ui.components.CyberCard
import com.fahimkhan.ava.ui.components.NeonButton
import com.fahimkhan.ava.ui.components.NeonText
import com.fahimkhan.ava.ui.components.StatusPill
import com.fahimkhan.ava.ui.theme.AvaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securitySystem: OwnerSecuritySystem,
    private val memory        : MemoryRepository
) : ViewModel() {
    val currentSession   = securitySystem.currentSession
    val isOwnerPresent   = securitySystem.isOwnerPresent
    val securityLogs     = memory.db.securityLogDao().getRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun grantTempTrust(name: String, minutes: Int = 60) {
        viewModelScope.launch { securitySystem.grantTemporaryTrust(name, minutes) }
    }

    fun revokeAccess() {
        viewModelScope.launch { securitySystem.revokeAccess() }
    }

    fun simulateIntruder() {
        viewModelScope.launch { securitySystem.flagAsIntruder() }
    }

    fun logTestEvent() {
        viewModelScope.launch {
            memory.logSecurityEvent("TEST_EVENT", "INFO", "Manual security test triggered")
        }
    }
}

@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel = hiltViewModel(),
    onBack   : () -> Unit
) {
    val session  by viewModel.currentSession.collectAsState()
    val isOwner  by viewModel.isOwnerPresent.collectAsState()
    val logs     by viewModel.securityLogs.collectAsState()

    // Pulsing alert animation
    val alertPulse = rememberInfiniteTransition(label = "alert")
    val alertAlpha by alertPulse.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse), label = "alertAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = AvaColors.NeonPink) }
                Spacer(Modifier.width(8.dp))
                Column {
                    NeonText("SECURITY CORE", fontSize = 20.sp, glowColor = AvaColors.NeonRed, color = AvaColors.TextPrimary, letterSpacing = 3.sp)
                    Text("NEURAL PROTECTION SYSTEM", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                StatusPill(text = if (isOwner) "OWNER" else "LOCKED", isActive = isOwner)
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Current session card
                item {
                    val trustColor = when (session.trustLevel) {
                        UserTrustLevel.OWNER    -> AvaColors.Active
                        UserTrustLevel.TRUSTED  -> AvaColors.OrbListening
                        UserTrustLevel.UNKNOWN  -> AvaColors.Warning
                        UserTrustLevel.INTRUDER -> AvaColors.NeonRed
                    }
                    CyberCard(glowColor = trustColor) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, null, tint = trustColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CURRENT SESSION", color = trustColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            SecurityInfoRow("Trust Level", session.trustLevel.name)
                            SecurityInfoRow("User",        session.personName.ifBlank { "Unknown" })
                            SecurityInfoRow("Type",        if (session.isTemporary) "Temporary" else "Permanent")
                            if (session.trustLevel == UserTrustLevel.INTRUDER) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AvaColors.NeonRed.copy(0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, AvaColors.NeonRed.copy(0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .alpha(alertAlpha)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warning, null, tint = AvaColors.NeonRed, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("⚠ INTRUDER ALERT — OWNER NOTIFIED", color = AvaColors.NeonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick actions
                item {
                    CyberCard(glowColor = AvaColors.CyberPurple.copy(0.5f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SECURITY ACTIONS", color = AvaColors.CyberPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NeonButton("GRANT TEMP TRUST", AvaColors.OrbListening, { viewModel.grantTempTrust("Guest") }, Modifier.weight(1f))
                                NeonButton("REVOKE ACCESS",    AvaColors.NeonRed,      viewModel::revokeAccess,               Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            NeonButton("LOG TEST EVENT", AvaColors.CyberPurple, viewModel::logTestEvent, Modifier.fillMaxWidth())
                        }
                    }
                }

                // Security features list
                item {
                    CyberCard(glowColor = AvaColors.NeonRed.copy(0.4f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PROTECTION LAYERS", color = AvaColors.NeonRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(12.dp))
                            SecurityFeatureRow(Icons.Rounded.Face,           "Face Recognition",      "ML Kit biometric ID",              false)
                            SecurityFeatureRow(Icons.Rounded.RecordVoiceOver,"Voice Authentication",  "Voice print matching",              false)
                            SecurityFeatureRow(Icons.Rounded.PhotoCamera,    "Intruder Capture",      "Auto-photo on unauthorized access", false)
                            SecurityFeatureRow(Icons.Rounded.Lock,           "Auto-Lock",             "Locks after inactivity",            true)
                            SecurityFeatureRow(Icons.Rounded.NotificationsActive, "Owner Alerts",     "Notifies owner of security events", true)
                            SecurityFeatureRow(Icons.Rounded.History,        "Security Logging",      "Full audit trail",                  true)
                        }
                    }
                }

                // Security log
                item {
                    Text(
                        "SECURITY LOG",
                        color         = AvaColors.NeonRed,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                if (logs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No security events logged", color = AvaColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(logs) { log -> SecurityLogRow(log) }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun SecurityInfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AvaColors.TextMuted,     fontSize = 12.sp)
        Text(value, color = AvaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SecurityFeatureRow(
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    label      : String,
    description: String,
    enabled    : Boolean
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = if (enabled) AvaColors.Active else AvaColors.TextMuted, modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label,       color = if (enabled) AvaColors.TextSecondary else AvaColors.TextMuted, fontSize = 12.sp)
            Text(description, color = AvaColors.TextMuted, fontSize = 10.sp)
        }
        Text(if (enabled) "ON" else "OFF", color = if (enabled) AvaColors.Active else AvaColors.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecurityLogRow(log: SecurityLog) {
    val color = when (log.severity) {
        "CRITICAL" -> AvaColors.NeonRed
        "WARNING"  -> AvaColors.Warning
        "INFO"     -> AvaColors.Active
        else       -> AvaColors.TextMuted
    }
    val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())

    CyberCard(glowColor = color.copy(0.3f)) {
        Row(
            modifier              = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.event,          color = color,               fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (log.description.isNotBlank())
                    Text(log.description, color = AvaColors.TextMuted, fontSize = 10.sp)
            }
            Text(fmt.format(Date(log.timestamp)), color = AvaColors.TextMuted, fontSize = 9.sp)
        }
    }
}
