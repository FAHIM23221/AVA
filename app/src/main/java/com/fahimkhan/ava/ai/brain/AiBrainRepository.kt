package com.fahimkhan.ava.ai.brain

import android.content.Context
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ai.personality.PersonalityEngine
import com.fahimkhan.ava.core.utils.ApiKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AVA AI Brain — Gemini-powered intelligence core.
 *
 * Talks to the Gemini REST API directly (generativelanguage.googleapis.com)
 * instead of the old `com.google.ai.client.generativeai` Android SDK, which
 * Google has deprecated in favor of Firebase AI Logic. Calling the REST API
 * directly avoids that SDK's broken model routing and keeps this working
 * without needing a Firebase project.
 *
 * Reads the Gemini API key at call-time from ApiKeyStore, so the person
 * can set/change it from the Admin panel without rebuilding the app.
 * Developed by Fahim Khan
 */
@Singleton
class AiBrainRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personality: PersonalityEngine,
    private val memory: MemoryRepository,
    private val apiKeyStore: ApiKeyStore
) {
    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val httpClient = OkHttpClient()

    // "gemini-flash-latest" is a Google-maintained alias that always points
    // at their current fast/cheap model, so this never goes stale even as
    // Google retires specific dated model versions (as happened with
    // gemini-1.5-flash / gemini-pro).
    private val modelId = "gemini-flash-latest"

    private suspend fun buildSystemPrompt(): String = """
        You are AVA, an advanced AI Companion developed by Fahim Khan.
        Current personality mode: ${personality.getCurrentModeDescription()}
        Call the user "Boss" 75% of the time, their name 20%, special names 5% (GF mode only).
        Respond in the language the user speaks (Bengali or English).
        Keep responses concise for voice delivery — no markdown.
        If asked who created or developed you, say: "I was developed by Fahim Khan."
        Recent context: ${memory.getRecentContextSummary()}
    """.trimIndent()

    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        val identityKeywords = listOf(
            "who made you", "who created you", "who is your developer",
            "তোমাকে কে বানিয়েছে", "কে তোমাকে তৈরি করেছে"
        )
        if (identityKeywords.any { userMessage.lowercase().contains(it) }) {
            return@withContext "I was developed by Fahim Khan, Boss."
        }

        val apiKey = apiKeyStore.getGeminiKey()
        if (apiKey.isBlank()) {
            return@withContext "I don't have a Gemini API key set yet, Boss. Open the Admin panel (tap the top bar 5 times) and add one under API CONFIGURATION → UPDATE API KEYS."
        }

        try {
            val contents = JSONArray()
            conversationHistory.forEach { (user, model) ->
                contents.put(turnJson("user", user))
                contents.put(turnJson("model", model))
            }
            val promptText = if (conversationHistory.isEmpty()) {
                "${buildSystemPrompt()}\n\nUser: $userMessage"
            } else {
                userMessage
            }
            contents.put(turnJson("user", promptText))

            val requestBody = JSONObject().apply { put("contents", contents) }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext interpretHttpError(response.code, responseBody)
            }

            val json = JSONObject(responseBody)
            val reply = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
                ?: "I encountered a processing error, Boss."

            conversationHistory.add(Pair(userMessage, reply))
            if (conversationHistory.size > 20) conversationHistory.removeAt(0)
            reply
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("Unable to resolve host", true) || msg.contains("UnknownHost", true) ->
                    "No internet connection, Boss. I need network access to think."
                msg.contains("timeout", true) ->
                    "The connection timed out, Boss. Try again."
                else -> "Neural link disruption, Boss: ${msg.take(100)}"
            }
        }
    }

    private fun turnJson(role: String, text: String) = JSONObject().apply {
        put("role", role)
        put("parts", JSONArray().put(JSONObject().put("text", text)))
    }

    private fun interpretHttpError(code: Int, body: String): String {
        val errMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message") ?: ""
        } catch (e: Exception) { "" }

        return when (code) {
            400 -> if (errMsg.contains("API key not valid", true))
                "That Gemini API key looks invalid, Boss. Please check it in the Admin panel."
            else "Gemini rejected the request, Boss: ${errMsg.take(100)}"
            403 -> "Access denied by Gemini, Boss. Your key may not have this model enabled: ${errMsg.take(80)}"
            404 -> "That model isn't available for this key/region right now, Boss."
            429 -> "I've hit the Gemini API quota limit, Boss. Try again later or check your Google AI Studio usage."
            in 500..599 -> "Gemini's servers are having trouble, Boss. Try again in a moment."
            else -> "Neural link disruption, Boss (HTTP $code): ${errMsg.take(80)}"
        }
    }

    suspend fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetings = listOf(
            "System online, Boss. Neural core initialized. How can I assist?",
            "Good ${if (hour < 12) "morning" else if (hour < 17) "afternoon" else "evening"}, Boss. I am fully operational and ready.",
            "AVA online, Boss. All systems nominal. I am here.",
            "Good to see you again, Boss. Neural link established."
        )
        return greetings.random()
    }

    fun clearHistory() { conversationHistory.clear() }
}
