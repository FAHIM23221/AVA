package com.fahimkhan.ava.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.ai.brain.AiBrainRepository
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.core.extensions.isBengali
import com.fahimkhan.ava.core.utils.AppLauncherController
import com.fahimkhan.ava.core.utils.DeviceControlSystem
import com.fahimkhan.ava.core.utils.LaunchResult
import com.fahimkhan.ava.ui.components.OrbState
import com.fahimkhan.ava.voice.engine.VoiceEngine
import com.fahimkhan.ava.voice.wake.WakeWordDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(val isLoading: Boolean = false, val errorMessage: String? = null)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val aiBrain: AiBrainRepository,
    private val memory: MemoryRepository,
    private val voiceEngine: VoiceEngine,
    private val wakeWord: WakeWordDetector,
    private val appLauncher: AppLauncherController,
    private val deviceControl: DeviceControlSystem
) : ViewModel() {
    private val _uiState       = MutableStateFlow(MainUiState())
    val uiState                = _uiState.asStateFlow()
    private val _isLoading     = MutableStateFlow(true)
    val isLoading              = _isLoading.asStateFlow()
    private val _orbState      = MutableStateFlow(OrbState.IDLE)
    val orbState               = _orbState.asStateFlow()
    private val _subtitleText  = MutableStateFlow("")
    val subtitleText           = _subtitleText.asStateFlow()
    private val _audioLevel    = MutableStateFlow(0f)
    val audioLevel             = _audioLevel.asStateFlow()
    private val _avaMood       = MutableStateFlow(AvaMood.CALM)
    val avaMood                = _avaMood.asStateFlow()
    private var isListening    = false

    // Guards against a single spoken utterance accidentally firing two
    // overlapping network requests (some devices' speech recognizers emit
    // a final result more than once for the same utterance).
    @Volatile private var isProcessing = false

    init {
        viewModelScope.launch { kotlinx.coroutines.delay(2000); _isLoading.value = false; greetUser() }
        observeVoiceEngine()
    }

    private fun observeVoiceEngine() {
        viewModelScope.launch { voiceEngine.audioLevelFlow.collect { _audioLevel.value = it } }
        viewModelScope.launch {
            voiceEngine.transcriptFlow.collect { transcript ->
                if (transcript.isNotBlank() && !isProcessing) {
                    handleUserInput(transcript)
                }
            }
        }
    }

    private fun greetUser() = viewModelScope.launch {
        _orbState.value = OrbState.SPEAKING
        val greeting = aiBrain.getGreeting()
        _subtitleText.value = greeting
        voiceEngine.speak(greeting)
        kotlinx.coroutines.delay(3000)
        _orbState.value = OrbState.IDLE; _subtitleText.value = ""
    }

    fun toggleListening() = viewModelScope.launch {
        if (isProcessing) return@launch // ignore taps while a request is already in flight
        isListening = !isListening
        if (isListening) { _orbState.value = OrbState.LISTENING; _subtitleText.value = "Listening..."; voiceEngine.startListening() }
        else { _orbState.value = OrbState.IDLE; _subtitleText.value = ""; voiceEngine.stopListening() }
    }

    private fun handleUserInput(input: String) {
        if (isProcessing) return // belt-and-suspenders: never process two utterances at once
        isProcessing = true

        viewModelScope.launch {
            try {
                isListening = false
                _orbState.value = OrbState.THINKING
                _subtitleText.value = input
                voiceEngine.stopListening()

                // Try to handle it as a local device command first (open app,
                // wifi/bluetooth/volume/flashlight) — these never touch Gemini.
                val response = handleDeviceCommand(input) ?: aiBrain.chat(input)

                memory.saveConversation(input, response)
                _orbState.value = OrbState.SPEAKING
                _subtitleText.value = response
                voiceEngine.speak(response)

                kotlinx.coroutines.delay(4000)
                _orbState.value = OrbState.IDLE
                _subtitleText.value = ""
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Returns a response string if [input] was handled as a device command
     * (app launch, wifi/bluetooth/volume/flashlight), or null if it should
     * be sent to Gemini instead.
     */
    private suspend fun handleDeviceCommand(input: String): String? {
        val appName = extractAppLaunchTarget(input)
        if (appName != null) {
            val bengali = input.isBengali()
            return when (val result = appLauncher.launchApp(appName)) {
                is LaunchResult.Success ->
                    if (bengali) "${result.appLabel} খুলছি, Boss।" else "Opening ${result.appLabel}, Boss."
                is LaunchResult.NotFound -> {
                    val suggestion = result.suggestions.firstOrNull()
                    when {
                        bengali && suggestion != null -> "আমি '$appName' নামে কোনো অ্যাপ খুঁজে পাইনি, Boss। তুমি কি '$suggestion' বলতে চেয়েছিলে?"
                        bengali -> "আমি '$appName' নামে কোনো অ্যাপ খুঁজে পাইনি আপনার ফোনে, Boss।"
                        suggestion != null -> "I couldn't find an app called '$appName', Boss. Did you mean '$suggestion'?"
                        else -> "I couldn't find an app called '$appName' on this device, Boss."
                    }
                }
            }
        }

        val deviceResponse = deviceControl.executeCommand(input)
        if (deviceResponse.isNotBlank()) return deviceResponse

        return null
    }

    /**
     * Pulls an app name out of open/launch phrasing, English + Bengali.
     * Unanchored (uses find(), not matchEntire()) so this also catches an
     * app-open instruction embedded inside a longer sentence, e.g.
     * "ইউটিউব ওপেন করে একটা রেন্ডম ভিডিও চালাও" — previously this only
     * matched when the ENTIRE message was just the open command, so any
     * trailing instruction (however AVA then can't actually carry out)
     * silently skipped the whole app-launch step and went to chat instead.
     */
    private fun extractAppLaunchTarget(text: String): String? {
        val trimmed = text.trim()

        // English: "open X", "launch X", "start X app" — stops at a
        // connector word so "open youtube and play a video" still yields
        // just "youtube".
        val englishMatch = Regex(
            "\\b(?:open|launch|start)\\s+(\\S+(?:\\s+\\S+)?)",
            RegexOption.IGNORE_CASE
        ).find(trimmed)
        if (englishMatch != null) {
            val candidate = englishMatch.groupValues[1]
                .split(Regex("\\b(?:and|then)\\b", RegexOption.IGNORE_CASE)).first()
                .trim()
            if (candidate.isNotBlank()) return candidate
        }

        // Bengali, app name first: "X খোলো/খোল", "X ওপেন কর(ো/ে)", "X চালু কর(ো/ে)"
        // — found anywhere in the sentence, not just when it's the whole message.
        val bengaliMatchAppFirst = Regex(
            "(\\S+(?:\\s+\\S+)?)\\s*(?:অ্যাপ(?:টি)?)?\\s*(?:ওপেন\\s*কর(?:ো|ে)?|খোল(?:ো)?|চালু\\s*কর(?:ো|ে)?)"
        ).find(trimmed)
        if (bengaliMatchAppFirst != null) {
            val candidate = bengaliMatchAppFirst.groupValues[1].trim()
            if (candidate.isNotBlank()) return candidate
        }

        // Bengali/transliterated, verb first: "ওপেন X", "ওপেন কর X", "লঞ্চ X", "স্টার্ট X".
        // Only takes one token here (not 1-2 like above) so a trailing
        // connector word like "তারপর" never gets swept into the app name.
        val bengaliMatchVerbFirst = Regex(
            "(?:ওপেন|লঞ্চ|স্টার্ট)\\s*(?:কর(?:ো|ে)?)?\\s+(\\S+)"
        ).find(trimmed)
        if (bengaliMatchVerbFirst != null) {
            val candidate = bengaliMatchVerbFirst.groupValues[1].trim()
            if (candidate.isNotBlank()) return candidate
        }

        return null
    }
}
