package com.fahimkhan.ava.ai.brain

import android.content.Context
import com.fahimkhan.ava.BuildConfig
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ai.personality.PersonalityEngine
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiBrainRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personality: PersonalityEngine,
    private val memory: MemoryRepository
) {
    private val model = GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.GEMINI_API_KEY)
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    private suspend fun buildSystemPrompt() = """
        You are AVA, an advanced AI Companion developed by Fahim Khan.
        Current personality mode: ${personality.getCurrentModeDescription()}
        Call the user "Boss" 75% of the time, their name 20%, special names 5% (GF mode only).
        Respond in the language the user speaks (Bengali or English).
        Keep responses concise for voice delivery — no markdown.
        If asked who created or developed you, say: "I was developed by Fahim Khan."
        Recent context: ${memory.getRecentContextSummary()}
    """.trimIndent()

    suspend fun chat(userMessage: String): String = withContext(Dispatchers.IO) {
        val identityKeywords = listOf("who made you","who created you","who is your developer","তোমাকে কে বানিয়েছে","কে তোমাকে তৈরি করেছে")
        if (identityKeywords.any { userMessage.lowercase().contains(it) }) return@withContext "I was developed by Fahim Khan, Boss."
        return@withContext try {
            val chat = model.startChat(history = conversationHistory.flatMap { (u, m) -> listOf(content("user") { text(u) }, content("model") { text(m) }) })
            val prompt = if (conversationHistory.isEmpty()) "${buildSystemPrompt()}\n\nUser: $userMessage" else userMessage
            val response = chat.sendMessage(prompt)
            val reply = response.text ?: "I encountered a processing error, Boss."
            conversationHistory.add(Pair(userMessage, reply))
            if (conversationHistory.size > 20) conversationHistory.removeAt(0)
            reply
        } catch (e: Exception) {
            listOf("Neural link disruption, Boss. Please try again.", "I am having trouble connecting, Boss. Check the network.").random()
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
