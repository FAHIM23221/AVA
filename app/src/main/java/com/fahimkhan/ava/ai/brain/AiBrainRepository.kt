package com.fahimkhan.ava.ai.brain

import android.content.Context
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ai.personality.PersonalityEngine
import com.fahimkhan.ava.core.utils.ApiKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AVA AI Brain — Gemini-powered intelligence core.
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

    private fun buildSystemPrompt(): String = """
        You are AVA, an advanced AI Companion developed by Fahim Khan.
        Current personality mode: ${personality.getCurrentModeDescription()}
        Call the user "Boss" 75% of the time, their name 20%, special names 5% (GF mode only).
        Respond in the language the user speaks (Bengali or English).
        Keep responses concise for voice delivery — no markdown.
        If asked who created or developed you, say: "I was developed by Fahim Khan."
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
            return@withContext "I don't have a Gemini API key set yet, Boss. Open the Admin panel (tap the top bar 5 times) and add one under AI BRAIN CONFIG → UPDATE API KEYS."
        }

        return@withContext try {
            val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
            val chat = model.startChat(
                history = conversationHistory.flatMap { (u, m) ->
                    listOf(content("user") { text(u) }, content("model") { text(m) })
                }
            )
            val recentContext = memory.getRecentContextSummary()
            val prompt = if (conversationHistory.isEmpty()) {
                "${buildSystemPrompt()}\nRecent context: $recentContext\n\nUser: $userMessage"
            } else {
                userMessage
            }
            val response = chat.sendMessage(prompt)
            val reply = response.text ?: "I encountered a processing error, Boss."
            conversationHistory.add(Pair(userMessage, reply))
            if (conversationHistory.size > 20) conversationHistory.removeAt(0)
            reply
        } catch (e: Exception) {
            val msg = e.message ?: ""
            when {
                msg.contains("API_KEY_INVALID", ignoreCase = true) ||
                msg.contains("API key not valid", ignoreCase = true) ->
                    "That Gemini API key looks invalid, Boss. Please check it in the Admin panel."
                msg.contains("quota", ignoreCase = true) ->
                    "I've hit the Gemini API quota limit, Boss. Try again later or check your Google AI Studio usage."
                msg.contains("UnknownHost", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                    "No internet connection, Boss. I need network access to think."
                else -> "Neural link disruption, Boss: ${msg.take(80)}"
            }
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
