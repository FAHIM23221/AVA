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
import java.util.concurrent.TimeUnit
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
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Pinned to a specific stable model rather than any "-latest" alias.
    // "-latest" silently follows Google's newest release, and newest often
    // means a much stingier (or zero) free-tier quota — confirmed twice by
    // direct testing: "gemini-flash-latest" resolved to gemini-3.8-flash
    // (20 free requests/day), and "gemini-2.5-flash" turned out to be
    // retired for this project entirely (HTTP 404, "no longer available
    // to new users"). Verified via curl against this project's actual key
    // that "gemini-3.5-flash-lite" returns 200 with plain text and no
    // extra hidden "thinking" tokens (unlike the non-lite preview models,
    // which burn dozens of reasoning tokens per reply and eat quota
    // faster). "Lite" models are Google's current high-volume/cheap tier,
    // which is exactly what a chat companion needs. If this ever gets
    // retired too, re-run the curl model-list + per-model test in
    // MOBILE_SETUP_GUIDE_BANGLA.md before picking a replacement — don't
    // guess from a name alone.
    private val modelId = "gemini-3.5-flash-lite"

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
            val promptText = if (conversationHistory.isEmpty()) {
                "${buildSystemPrompt()}\n\nUser: $userMessage"
            } else {
                userMessage
            }

            var httpResult = callGemini(apiKey, promptText)

            // A 429 can be a brief burst limit rather than a hard daily cap —
            // wait a moment and try exactly once more before giving up.
            if (httpResult is GeminiCallResult.HttpError && httpResult.code == 429) {
                kotlinx.coroutines.delay(3000)
                httpResult = callGemini(apiKey, promptText)
            }

            when (httpResult) {
                is GeminiCallResult.Success -> {
                    conversationHistory.add(Pair(userMessage, httpResult.text))
                    // Keep history short: fewer tokens per request = less
                    // chance of hitting per-minute token quota, and a lower
                    // bill even on paid keys.
                    if (conversationHistory.size > 6) conversationHistory.removeAt(0)
                    httpResult.text
                }
                is GeminiCallResult.HttpError -> interpretHttpError(httpResult.code, httpResult.body)
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("Unable to resolve host", true) || msg.contains("UnknownHost", true) ->
                    "No internet connection, Boss. I need network access to think."
                msg.contains("timeout", true) ->
                    "The connection timed out, Boss (${e.javaClass.simpleName}). Try again."
                else -> "Neural link disruption, Boss (${e.javaClass.simpleName}): ${msg.take(100)}"
            }
        }
    }

    private sealed class GeminiCallResult {
        data class Success(val text: String) : GeminiCallResult()
        data class HttpError(val code: Int, val body: String) : GeminiCallResult()
    }

    private fun callGemini(apiKey: String, promptText: String): GeminiCallResult {
        val contents = JSONArray()
        conversationHistory.forEach { (user, model) ->
            contents.put(turnJson("user", user))
            contents.put(turnJson("model", model))
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
            return GeminiCallResult.HttpError(response.code, responseBody)
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

        return GeminiCallResult.Success(reply)
    }

    private fun turnJson(role: String, text: String) = JSONObject().apply {
        put("role", role)
        put("parts", JSONArray().put(JSONObject().put("text", text)))
    }

    private fun interpretHttpError(code: Int, body: String): String {
        val errMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message") ?: ""
        } catch (e: Exception) { "" }

        // If the body wasn't valid JSON, this response likely didn't come
        // from Google at all (a carrier/proxy block page, captive portal,
        // etc). Surfacing a raw snippet makes that visible instead of
        // hiding it behind a generic "server trouble" message.
        val rawSnippet = if (errMsg.isBlank() && body.isNotBlank()) " [raw: ${body.take(120)}]" else ""

        return when (code) {
            400 -> if (errMsg.contains("API key not valid", true))
                "That Gemini API key looks invalid, Boss. Please check it in the Admin panel."
            else "Gemini rejected the request, Boss (HTTP 400): ${errMsg.take(100)}$rawSnippet"
            403 -> "Access denied by Gemini, Boss (HTTP 403): ${errMsg.take(80)}$rawSnippet"
            404 -> "That model isn't available for this key/region right now, Boss (HTTP 404).$rawSnippet"
            429 -> "I've hit the Gemini API quota limit, Boss (HTTP 429). Try again later or check your Google AI Studio usage.$rawSnippet"
            in 500..599 -> "Gemini's servers are having trouble, Boss (HTTP $code).$rawSnippet"
            else -> "Neural link disruption, Boss (HTTP $code): ${errMsg.take(80)}$rawSnippet"
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
