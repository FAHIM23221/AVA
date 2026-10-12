package com.fahimkhan.ava.voice.wake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeWordDetector @Inject constructor(@ApplicationContext private val context: Context) {
    companion object { val WAKE_WORDS = setOf("ava", "AVA", "আভা") }

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected = _wakeWordDetected.asSharedFlow()

    private var recognizer: SpeechRecognizer? = null
    private var isActive = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startContinuousListening() { if (isActive) return; isActive = true; createAndStartRecognizer() }
    fun stopContinuousListening() { isActive = false; recognizer?.destroy(); recognizer = null }

    private fun createAndStartRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(e: Int) { if (isActive) scope.launch { delay(1000); createAndStartRecognizer() } }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                val detected = matches.any { r -> WAKE_WORDS.any { w -> r.lowercase().contains(w.lowercase()) } }
                if (detected) scope.launch { _wakeWordDetected.emit(Unit) }
                if (isActive) createAndStartRecognizer()
            }
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(p: Int, p1: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { recognizer?.startListening(intent) } catch (e: Exception) {}
    }
}
