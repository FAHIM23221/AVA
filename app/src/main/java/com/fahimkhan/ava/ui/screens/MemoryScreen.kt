package com.fahimkhan.ava.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.ai.memory.ConversationMemory
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ui.components.CyberCard
import com.fahimkhan.ava.ui.components.NeonText
import com.fahimkhan.ava.ui.theme.AvaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(private val repo: MemoryRepository) : ViewModel() {
    val conversations = repo.db.conversationDao().getRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val shortTermMems = repo.db.shortTermDao().getRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    fun clearAll() = viewModelScope.launch { repo.clearAll() }
}

@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel = hiltViewModel(),
    onBack   : () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    var selectedTab   by remember { mutableStateOf(0) }
    val tabs = listOf("CONVERSATIONS", "SHORT-TERM", "FACES")

    Box(modifier = Modifier.fillMaxSize().background(AvaColors.BlackAbyss)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = AvaColors.NeonPink)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    NeonText("MEMORY CORE", fontSize = 20.sp, glowColor = AvaColors.IceCyan, color = AvaColors.TextPrimary, letterSpacing = 3.sp)
                    Text("NEURAL STORAGE SYSTEM", color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                }
            }

            // Tabs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, tab ->
                    val active = i == selectedTab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (active) AvaColors.IceCyan.copy(0.15f) else AvaColors.CardSurface,
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (active) AvaColors.IceCyan.copy(0.6f) else AvaColors.ElevatedSurface, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = i }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(tab, color = if (active) AvaColors.IceCyan else AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> ConversationList(conversations)
                1 -> ShortTermList(viewModel)
                2 -> FaceMemoryList()
            }
        }
    }
}

@Composable
fun ConversationList(conversations: List<ConversationMemory>) {
    if (conversations.isEmpty()) {
        EmptyMemoryState("No conversations recorded yet")
    } else {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations) { conv ->
                CyberCard(glowColor = AvaColors.IceCyan.copy(0.5f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val fmt  = SimpleDateFormat("MMM dd  HH:mm", Locale.getDefault())
                        val time = fmt.format(Date(conv.timestamp))
                        Text(time, color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("YOU", color = AvaColors.NeonPink, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(conv.userMessage, color = AvaColors.TextPrimary, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AVA", color = AvaColors.IceCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(conv.avaResponse, color = AvaColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortTermList(viewModel: MemoryViewModel) {
    val mems by viewModel.shortTermMems.collectAsState()
    if (mems.isEmpty()) {
        EmptyMemoryState("Short-term memory is empty")
    } else {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mems) { mem ->
                CyberCard(glowColor = AvaColors.CyberPurple.copy(0.4f)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, null, tint = AvaColors.CyberPurple, modifier = Modifier.size(14.dp))
                        Text(mem.content, color = AvaColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(mem.category.uppercase(), color = AvaColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FaceMemoryList() {
    EmptyMemoryState("No face profiles registered yet.\nUse Vision mode to register trusted people.")
}

@Composable
fun EmptyMemoryState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.DataObject, null, tint = AvaColors.TextMuted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = AvaColors.TextMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
