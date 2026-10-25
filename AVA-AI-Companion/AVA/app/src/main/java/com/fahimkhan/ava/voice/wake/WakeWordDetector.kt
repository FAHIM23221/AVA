package com.fahimkhan.ava.voice.wake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.fahimkhan.ava.core.utils.ApiKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake Word Detector — Listens for "AVA" to activate.
 * Uses Android SpeechRecognizer in continuous mode.
 *
 * Known limitation: Android's stock SpeechRecognizer has no true
 * "always listening, never stops" mode — each session ends after a pause
 * and has to be recreated and restarted, which is why the system's
 * mic-in-use indicator ("Speech Recognition and Synthesis from Google")
 * flickers on/off repeatedly while this runs in the background. A fully
 * flicker-free always-on listener needs a dedicated on-device wake-word
 * engine (e.g. Porcupine) instead of the stock recognizer — that's a
 * separate, larger integration, not a quick fix. What's done here is
 * reduce how often it restarts, not eliminate the flicker entirely.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore
) {
    companion object {
        val WAKE_WORDS = setOf("ava", "AVA", "আভা")
        private const val RESTART_DELAY_MS = 1500L
    }

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected = _wakeWordDetected.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null
    private var isActive = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startContinuousListening() {
        if (isActive) return
        isActive = true
        scope.launch { createAndStartRecognizer() }
    }

    fun stopContinuousListening() {
        isActive = false
        recognizer?.destroy()
        recognizer = null
    }

    private suspend fun createAndStartRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) {
                if (isActive) scope.launch { delay(RESTART_DELAY_MS); createAndStartRecognizer() }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                val detected = matches.any { r -> WAKE_WORDS.any { w -> r.lowercase().contains(w.lowercase()) } }
                if (detected) scope.launch { _wakeWordDetected.emit(Unit) }
                if (isActive) scope.launch { delay(RESTART_DELAY_MS); createAndStartRecognizer() }
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(p: Int, p1: Bundle?) {}
        })

        val languageTag = apiKeyStore.getRecognitionLanguage()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (e: Exception) { /* no recognizer available */ }
    }
}
