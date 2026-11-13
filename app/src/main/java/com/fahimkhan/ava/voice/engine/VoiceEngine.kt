package com.fahimkhan.ava.voice.engine

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.fahimkhan.ava.core.extensions.isBengali
import com.fahimkhan.ava.core.utils.ApiKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AVA Voice Engine
 * - Speech recognition via Android SpeechRecognizer
 * - TTS via ElevenLabs API (if configured) + Android TTS (fallback)
 * Developed by Fahim Khan
 */
@Singleton
class VoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore
) : TextToSpeech.OnInitListener {

    private val _audioLevelFlow = MutableStateFlow(0f)
    val audioLevelFlow = _audioLevelFlow.asStateFlow()

    private val _transcriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val transcriptFlow = _transcriptFlow.asSharedFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private var androidTts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var geminiTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init { initAndroidTts(); initSpeechRecognizer() }

    private fun initAndroidTts() { androidTts = TextToSpeech(context, this) }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Default to English; the actual locale is now chosen per
            // message in speakWithAndroidTts() based on the text's
            // language, since a response can switch between Bengali and
            // English turn to turn.
            androidTts?.language = Locale.US
            androidTts?.setSpeechRate(0.95f)
            androidTts?.setPitch(0.9f)
            ttsReady = true
            androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { _audioLevelFlow.value = 0f }
                @Deprecated("Deprecated") override fun onError(id: String?) {}
            })
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { _isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { _audioLevelFlow.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
                override fun onBufferReceived(p: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false; _audioLevelFlow.value = 0f }
                override fun onError(error: Int) { _isListening.value = false; _audioLevelFlow.value = 0f }
                override fun onResults(results: Bundle?) {
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    scope.launch { _transcriptFlow.emit(t) }
                }
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(p: Int, p1: Bundle?) {}
            })
        }
    }

    /**
     * Must be called only after RECORD_AUDIO permission has been granted.
     * Listens in whichever single language is set in Settings → Voice &
     * Language (ApiKeyStore.getRecognitionLanguage(), default "bn-BD").
     * Android's on-device recognizer can only target one language per
     * session — there's no built-in "detect whatever language was
     * spoken" the way some cloud speech services offer — so if the
     * person switches languages often, they need to change this setting
     * to match, rather than it being guessed automatically.
     */
    suspend fun startListening() {
        val languageTag = apiKeyStore.getRecognitionLanguage()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try { speechRecognizer?.startListening(intent) } catch (e: Exception) { /* permission missing or no recognizer */ }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        _audioLevelFlow.value = 0f
    }

    /**
     * Voice priority: ElevenLabs (if configured) → Gemini's own TTS with a
     * female voice like "Aoede" (same Gemini key; handles Bengali and most
     * other languages properly) → the phone's built-in voice as the last
     * resort if the network call fails or quota runs out. The Gemini /
     * ElevenLabs paths return once playback has finished.
     */
    suspend fun speak(text: String) {
        val useElevenLabs = apiKeyStore.getUseElevenLabs() && apiKeyStore.hasElevenLabsKey()
        when {
            useElevenLabs -> speakWithElevenLabs(text)
            apiKeyStore.getUseGeminiVoice() && speakWithGeminiTts(text) -> Unit
            else -> speakWithAndroidTts(text)
        }
    }

    // Tried in order; the first that answers with audio wins. The first is
    // the one verified working on this project's key.
    private val geminiTtsModels = listOf("gemini-3.8-flash-lite-tts", "gemini-2.5-flash-preview-tts")

    // Gemini replies sometimes contain markdown (**bold**, #, `code`) that a
    // voice would read out literally.
    private fun cleanForSpeech(text: String): String =
        text.replace(Regex("[*#`_~]+"), "").replace(Regex("\\s+"), " ").trim()

    /** Returns true if Gemini TTS produced audio and it finished playing. */
    private suspend fun speakWithGeminiTts(text: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = apiKeyStore.getGeminiKey()
        if (apiKey.isBlank()) return@withContext false
        val voiceName = apiKeyStore.getGeminiVoiceName()
        val spoken = cleanForSpeech(text)
        if (spoken.isBlank()) return@withContext false

        for (model in geminiTtsModels) {
            try {
                val body = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(
                        // The part before the colon is a style instruction, not spoken.
                        JSONObject().put("text", "Say in a warm, natural, friendly tone: $spoken")
                    ))))
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put("speechConfig", JSONObject().put("voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName))))
                    })
                }
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) continue

                val inline = JSONObject(responseBody).optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)
                    ?.optJSONObject("inlineData") ?: continue
                val pcm = Base64.decode(inline.optString("data"), Base64.DEFAULT)
                if (pcm.isEmpty()) continue
                val sampleRate = Regex("rate=(\\d+)").find(inline.optString("mimeType"))
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 24000

                playPcm(pcm, sampleRate)
                return@withContext true
            } catch (e: Exception) {
                // network/parse problem — try the next model, then fall back
            }
        }
        false
    }

    private suspend fun playPcm(pcm: ByteArray, sampleRate: Int) {
        stopGeminiPlayback()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        geminiTrack = track
        track.write(pcm, 0, pcm.size)
        track.play()
        _audioLevelFlow.value = 0.7f
        delay(pcm.size * 1000L / (sampleRate * 2) + 200)
        _audioLevelFlow.value = 0f
        if (geminiTrack === track) stopGeminiPlayback()
    }

    private fun stopGeminiPlayback() {
        geminiTrack?.let { t ->
            try { t.stop() } catch (e: Exception) { /* already stopped */ }
            try { t.release() } catch (e: Exception) { /* already released */ }
        }
        geminiTrack = null
    }

    private fun speakWithAndroidTts(text: String) {
        if (!ttsReady) return

        // Pick the TTS voice/locale to match THIS message's language —
        // previously this was set once at startup to US English and never
        // changed, so Bengali replies were read with an English voice
        // engine, badly mangling pronunciation (e.g. "আভা" coming out as
        // "Eva"/"Yeva"). A Bengali voice may not be installed on every
        // device; if setting it fails/is unsupported, fall back to
        // whatever was already active rather than silently saying nothing.
        val bengali = text.isBengali()
        val targetLocale = if (bengali) Locale("bn", "BD") else Locale.US
        val result = androidTts?.setLanguage(targetLocale)
        if (bengali && (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
            // No Bengali voice installed on this device — try the generic
            // Bengali locale (no country) before giving up and speaking
            // with whatever voice is already active.
            androidTts?.setLanguage(Locale("bn"))
        }
        preferFemaleVoice(targetLocale)

        val spokenText = if (bengali) normalizeForBengaliSpeech(text) else text
        androidTts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "AVA_${System.currentTimeMillis()}")
        scope.launch { _audioLevelFlow.value = 0.6f; delay(text.length * 60L); _audioLevelFlow.value = 0f }
    }

    // Best-effort pick of a female-sounding voice for the given locale.
    // Android's TextToSpeech.Voice has no standard "gender" field — this
    // is a heuristic based on how Google's and most third-party TTS
    // engines name their voices (e.g. "bn-bd-x-bnd-local", "en-us-x-sfg
    // -local#female_1"). It only changes anything if the installed engine
    // actually ships more than one voice for this locale; many devices
    // only have one. For a guaranteed, consistently good female voice —
    // the "MYRA sounds cuter" difference — the reliable fix is enabling
    // ElevenLabs in the Admin panel (API CONFIGURATION), which lets you
    // pick an exact voice instead of whatever the phone happens to ship.
    private fun preferFemaleVoice(locale: Locale) {
        try {
            val voices = androidTts?.voices ?: return
            val candidate = voices.firstOrNull { voice ->
                voice.locale.language == locale.language &&
                    (voice.name.contains("female", ignoreCase = true) ||
                     voice.name.contains("#female", ignoreCase = true) ||
                     voice.name.contains("_f_", ignoreCase = true))
            }
            if (candidate != null) androidTts?.voice = candidate
        } catch (e: Exception) { /* engine doesn't expose voice list; keep default */ }
    }

    // Words that read fine in an English sentence but get mispronounced by
    // a Bengali TTS voice when left in Latin script (it tries to sound out
    // English letters using Bengali phonetics). Swapping in the Bengali
    // spelling fixes pronunciation without changing what's shown on screen
    // or saved to memory — this only affects what's sent to the speech
    // engine.
    private val bengaliSpeechSubstitutions = listOf(
        "\\bAVA\\b".toRegex(RegexOption.IGNORE_CASE) to "আভা",
        "\\bBoss\\b".toRegex(RegexOption.IGNORE_CASE) to "বস"
    )

    private fun normalizeForBengaliSpeech(text: String): String {
        var result = text
        bengaliSpeechSubstitutions.forEach { (pattern, replacement) ->
            result = result.replace(pattern, replacement)
        }
        return result
    }

    private suspend fun speakWithElevenLabs(text: String) = withContext(Dispatchers.IO) {
        try {
            val apiKey  = apiKeyStore.getElevenLabsKey()
            val voiceId = apiKeyStore.getElevenLabsVoiceId()

            val json = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.8)
                })
            }

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) playAudioBytes(bytes) else speakWithAndroidTts(text)
            } else {
                speakWithAndroidTts(text)
            }
        } catch (e: Exception) {
            speakWithAndroidTts(text)
        }
    }

    private fun playAudioBytes(bytes: ByteArray) {
        val file = java.io.File(context.cacheDir, "ava_speech.mp3")
        file.writeBytes(bytes)
        val mp = android.media.MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() }
        scope.launch { _audioLevelFlow.value = 0.7f; delay(mp.duration.toLong()); _audioLevelFlow.value = 0f; mp.release() }
    }

    fun stopSpeaking() { androidTts?.stop(); stopGeminiPlayback(); _audioLevelFlow.value = 0f }
    fun destroy() { speechRecognizer?.destroy(); androidTts?.shutdown(); scope.cancel() }
}
