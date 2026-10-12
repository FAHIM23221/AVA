package com.fahimkhan.ava.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahimkhan.ava.ai.brain.AiBrainRepository
import com.fahimkhan.ava.ai.memory.MemoryRepository
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
    private val wakeWord: WakeWordDetector
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

    init {
        viewModelScope.launch { kotlinx.coroutines.delay(2000); _isLoading.value = false; greetUser() }
        observeVoiceEngine()
    }

    private fun observeVoiceEngine() {
        viewModelScope.launch { voiceEngine.audioLevelFlow.collect { _audioLevel.value = it } }
        viewModelScope.launch { voiceEngine.transcriptFlow.collect { if (it.isNotBlank()) handleUserInput(it) } }
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
        isListening = !isListening
        if (isListening) { _orbState.value = OrbState.LISTENING; _subtitleText.value = "Listening..."; voiceEngine.startListening() }
        else { _orbState.value = OrbState.IDLE; _subtitleText.value = ""; voiceEngine.stopListening() }
    }

    private fun handleUserInput(input: String) = viewModelScope.launch {
        isListening = false; _orbState.value = OrbState.THINKING; _subtitleText.value = input; voiceEngine.stopListening()
        val response = aiBrain.chat(input); memory.saveConversation(input, response)
        _orbState.value = OrbState.SPEAKING; _subtitleText.value = response; voiceEngine.speak(response)
        kotlinx.coroutines.delay(4000); _orbState.value = OrbState.IDLE; _subtitleText.value = ""
    }
}
