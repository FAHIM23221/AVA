package com.fahimkhan.ava.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.admin.AdminRepository
import com.fahimkhan.ava.core.utils.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminState(
    val neuralCoreStatus: String = "ONLINE", val voiceEngineStatus: String = "ONLINE",
    val memoryStatus: String = "ONLINE", val securityStatus: String = "ONLINE",
    val visionStatus: String = "STANDBY", val wakeWordStatus: String = "ONLINE",
    val geminiEnabled: Boolean = true, val elevenLabsEnabled: Boolean = false,
    val bengaliEnabled: Boolean = true, val emotionalMode: Boolean = true,
    val gfModeEnabled: Boolean = false, val professionalMode: Boolean = false,
    val ownerRecognitionOn: Boolean = false, val intruderDetection: Boolean = false,
    val securityLogging: Boolean = true, val autoLock: Boolean = true,
    val alwaysListening: Boolean = false, val wakeWordActive: Boolean = true,
    val autoStartBoot: Boolean = false,
    val shortTermCount: Int = 0, val longTermCount: Int = 0,
    val faceProfileCount: Int = 0, val conversationCount: Int = 0,
    val geminiKeyMasked: String = "Not set",
    val elevenLabsKeyMasked: String = "Not set",
    val hasGeminiKey: Boolean = false,
    val saveMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepo: AdminRepository,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()
    private val _adminState = MutableStateFlow(AdminState())
    val adminState = _adminState.asStateFlow()

    fun authenticate(password: String) = viewModelScope.launch {
        val ok = adminRepo.verifyPassword(password)
        _isAuthenticated.value = ok
        if (ok) {
            val stats = adminRepo.getSystemStats()
            refreshKeyDisplay()
            _adminState.update {
                it.copy(
                    shortTermCount = stats.shortTermCount,
                    longTermCount = stats.longTermCount,
                    conversationCount = stats.conversationCount
                )
            }
        }
    }

    private suspend fun refreshKeyDisplay() {
        val gemini = apiKeyStore.getGeminiKey()
        val eleven = apiKeyStore.getElevenLabsKey()
        val useEleven = apiKeyStore.getUseElevenLabs()
        _adminState.update {
            it.copy(
                geminiKeyMasked = apiKeyStore.maskKey(gemini),
                elevenLabsKeyMasked = apiKeyStore.maskKey(eleven),
                hasGeminiKey = gemini.isNotBlank(),
                elevenLabsEnabled = useEleven
            )
        }
    }

    /** Called from the "UPDATE API KEYS" dialog in Admin panel. */
    fun saveApiKeys(geminiKey: String, elevenLabsKey: String, elevenLabsVoiceId: String, useElevenLabs: Boolean) {
        viewModelScope.launch {
            if (geminiKey.isNotBlank()) apiKeyStore.saveGeminiKey(geminiKey)
            if (elevenLabsKey.isNotBlank() || elevenLabsVoiceId.isNotBlank()) {
                apiKeyStore.saveElevenLabsConfig(elevenLabsKey, elevenLabsVoiceId, useElevenLabs)
            }
            refreshKeyDisplay()
            _adminState.update { it.copy(saveMessage = "✓ API keys saved") }
        }
    }

    fun clearSaveMessage() = _adminState.update { it.copy(saveMessage = null) }

    fun toggleGemini(v: Boolean)            = _adminState.update { it.copy(geminiEnabled = v) }
    fun toggleElevenLabs(v: Boolean) {
        _adminState.update { it.copy(elevenLabsEnabled = v) }
        viewModelScope.launch {
            val eleven = apiKeyStore.getElevenLabsKey()
            val voice  = apiKeyStore.getElevenLabsVoiceId()
            apiKeyStore.saveElevenLabsConfig(eleven, voice, v)
        }
    }
    fun toggleBengali(v: Boolean)           = _adminState.update { it.copy(bengaliEnabled = v) }
    fun toggleEmotionalMode(v: Boolean)     = _adminState.update { it.copy(emotionalMode = v) }
    fun toggleGfMode(v: Boolean)            = _adminState.update { it.copy(gfModeEnabled = v) }
    fun toggleProfessionalMode(v: Boolean)  = _adminState.update { it.copy(professionalMode = v) }
    fun toggleOwnerRecognition(v: Boolean)  = _adminState.update { it.copy(ownerRecognitionOn = v) }
    fun toggleIntruderDetection(v: Boolean) = _adminState.update { it.copy(intruderDetection = v) }
    fun toggleSecurityLogging(v: Boolean)   = _adminState.update { it.copy(securityLogging = v) }
    fun toggleAutoLock(v: Boolean)          = _adminState.update { it.copy(autoLock = v) }
    fun toggleAlwaysListening(v: Boolean)   = _adminState.update { it.copy(alwaysListening = v) }
    fun toggleWakeWord(v: Boolean)          = _adminState.update { it.copy(wakeWordActive = v) }
    fun toggleAutoStart(v: Boolean)         = _adminState.update { it.copy(autoStartBoot = v) }
    fun clearAllMemory() = viewModelScope.launch {
        adminRepo.clearAllMemory()
        _adminState.update { it.copy(shortTermCount = 0, longTermCount = 0, conversationCount = 0) }
    }
}
