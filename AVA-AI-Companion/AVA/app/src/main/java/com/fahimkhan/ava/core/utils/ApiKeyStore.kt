package com.fahimkhan.ava.core.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fahimkhan.ava.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiKeyDataStore by preferencesDataStore("ava_api_keys")

/**
 * Runtime-editable API key storage.
 * Lets the person set/change Gemini + ElevenLabs keys from inside the app
 * (Admin panel) without rebuilding the APK.
 *
 * Falls back to the value baked into BuildConfig (set in app/build.gradle)
 * if nothing has been saved at runtime yet.
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GEMINI_KEY        = stringPreferencesKey("gemini_api_key")
        val ELEVENLABS_KEY    = stringPreferencesKey("elevenlabs_api_key")
        val ELEVENLABS_VOICE  = stringPreferencesKey("elevenlabs_voice_id")
        val USE_ELEVENLABS    = booleanPreferencesKey("use_elevenlabs")
        val RECOGNITION_LANG  = stringPreferencesKey("recognition_language")
    }

    private fun isPlaceholder(value: String) =
        value.isBlank() || value.startsWith("YOUR_")

    suspend fun getGeminiKey(): String {
        val saved = context.apiKeyDataStore.data.first()[Keys.GEMINI_KEY]
        if (!saved.isNullOrBlank()) return saved
        return BuildConfig.GEMINI_API_KEY.takeIf { !isPlaceholder(it) } ?: ""
    }

    suspend fun getElevenLabsKey(): String {
        val saved = context.apiKeyDataStore.data.first()[Keys.ELEVENLABS_KEY]
        if (!saved.isNullOrBlank()) return saved
        return BuildConfig.ELEVENLABS_API_KEY.takeIf { !isPlaceholder(it) } ?: ""
    }

    suspend fun getElevenLabsVoiceId(): String {
        val saved = context.apiKeyDataStore.data.first()[Keys.ELEVENLABS_VOICE]
        if (!saved.isNullOrBlank()) return saved
        return BuildConfig.ELEVENLABS_VOICE_ID.takeIf { !isPlaceholder(it) } ?: ""
    }

    suspend fun getUseElevenLabs(): Boolean =
        context.apiKeyDataStore.data.first()[Keys.USE_ELEVENLABS] ?: false

    suspend fun hasGeminiKey(): Boolean = getGeminiKey().isNotBlank()
    suspend fun hasElevenLabsKey(): Boolean = getElevenLabsKey().isNotBlank() && getElevenLabsVoiceId().isNotBlank()

    suspend fun saveGeminiKey(key: String) {
        context.apiKeyDataStore.edit { it[Keys.GEMINI_KEY] = key.trim() }
    }

    suspend fun saveElevenLabsConfig(apiKey: String, voiceId: String, enabled: Boolean) {
        context.apiKeyDataStore.edit {
            it[Keys.ELEVENLABS_KEY]   = apiKey.trim()
            it[Keys.ELEVENLABS_VOICE] = voiceId.trim()
            it[Keys.USE_ELEVENLABS]   = enabled
        }
    }

    /**
     * The BCP-47 tag SpeechRecognizer should listen for (e.g. "bn-BD",
     * "en-US", "id-ID"). Previously this was hardcoded to Bengali
     * everywhere, so speaking any other language (Indonesian, Hindi,
     * plain English, etc.) was never recognized correctly. Android's
     * on-device recognizer only understands ONE language per session —
     * there's no true "auto-detect any spoken language" without a paid
     * cloud speech API — so this makes that one language a setting
     * instead of a hardcoded guess.
     */
    suspend fun getRecognitionLanguage(): String =
        context.apiKeyDataStore.data.first()[Keys.RECOGNITION_LANG] ?: "bn-BD"

    suspend fun saveRecognitionLanguage(languageTag: String) {
        context.apiKeyDataStore.edit { it[Keys.RECOGNITION_LANG] = languageTag.trim() }
    }

    /** Masked preview for display in Admin panel, e.g. "AIza••••••••7f2a" */
    fun maskKey(key: String): String {
        if (key.isBlank()) return "Not set"
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}${"•".repeat(8)}${key.takeLast(4)}"
    }
}
