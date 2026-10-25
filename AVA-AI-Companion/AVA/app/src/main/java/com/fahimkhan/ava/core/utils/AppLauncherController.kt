package com.fahimkhan.ava.core.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class LaunchableApp(val label: String, val packageName: String)

sealed class LaunchResult {
    data class Success(val appLabel: String) : LaunchResult()
    data class NotFound(val query: String, val suggestions: List<String>) : LaunchResult()
}

/**
 * Lets AVA actually open other apps on the device — the "device control"
 * feature the Admin panel's device-control system never had wired in.
 * Developed by Fahim Khan
 */
@Singleton
class AppLauncherController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager get() = context.packageManager

    /** All apps that show up on the home screen / app drawer. */
    fun getLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos.mapNotNull { info ->
            try {
                LaunchableApp(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = info.activityInfo.packageName
                )
            } catch (e: Exception) { null }
        }.distinctBy { it.packageName }
    }

    // Common Bengali names/transliterations for popular apps. Bengali script
    // and Latin script never share characters, so a plain "contains" match
    // against the installed app's (Latin) label can never succeed on its
    // own — this table bridges that gap for the apps people actually ask
    // for by voice. Extend as needed; anything not listed here still works
    // fine if spoken in English/the app's actual name.
    private val bengaliAppNames = mapOf(
        "ইউটিউব" to "youtube",
        "ফেসবুক" to "facebook",
        "হোয়াটসঅ্যাপ" to "whatsapp",
        "ওয়াটসঅ্যাপ" to "whatsapp",
        "ইনস্টাগ্রাম" to "instagram",
        "মেসেঞ্জার" to "messenger",
        "ক্যামেরা" to "camera",
        "গ্যালারি" to "gallery",
        "সেটিংস" to "settings",
        "ক্রোম" to "chrome",
        "ব্রাউজার" to "chrome",
        "ক্যালকুলেটর" to "calculator",
        "ক্যালেন্ডার" to "calendar",
        "জিমেইল" to "gmail",
        "ইমেইল" to "gmail",
        "ম্যাপ" to "maps",
        "মানচিত্র" to "maps",
        "প্লে স্টোর" to "play store",
        "টেলিগ্রাম" to "telegram",
        "টিকটক" to "tiktok",
        "স্পটিফাই" to "spotify",
        "ফোন" to "phone",
        "ডায়াল" to "phone",
        "মেসেজ" to "messages",
        "এসএমএস" to "messages"
    )

    private fun normalizeAppQuery(spokenName: String): String {
        val trimmed = spokenName.trim()
        // Check whole-phrase match first (handles multi-word entries like "প্লে স্টোর").
        bengaliAppNames[trimmed]?.let { return it }
        // Otherwise swap any individual Bengali words we recognize, leaving
        // anything else (including already-English text) untouched.
        return trimmed.split(" ").joinToString(" ") { word -> bengaliAppNames[word] ?: word }
    }

    /**
     * Finds the best-matching installed app for a spoken name and launches it.
     * Matching is tolerant: exact label match wins, then "contains" in either
     * direction. Common Bengali app names are translated to their English
     * equivalent first via [normalizeAppQuery], since Bengali script can
     * never textually match a Latin-script app label.
     */
    fun launchApp(spokenName: String): LaunchResult {
        val query = normalizeAppQuery(spokenName).trim().lowercase()
        if (query.isBlank()) return LaunchResult.NotFound(spokenName, emptyList())

        val apps = getLaunchableApps()

        val exact = apps.firstOrNull { it.label.lowercase() == query }
        val containsMatch = apps.firstOrNull {
            it.label.lowercase().contains(query) || query.contains(it.label.lowercase())
        }
        val match = exact ?: containsMatch

        if (match != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(match.packageName)
            return if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                LaunchResult.Success(match.label)
            } else {
                LaunchResult.NotFound(spokenName, emptyList())
            }
        }

        // Suggest closest-sounding installed apps so AVA can offer alternatives.
        val suggestions = apps
            .map { it.label }
            .filter { label -> query.split(" ").any { word -> word.length > 2 && label.lowercase().contains(word) } }
            .take(3)

        return LaunchResult.NotFound(spokenName, suggestions)
    }

    /**
     * Opens YouTube directly to search results for [query] (deep-linked
     * into the YouTube app itself when it's installed, not a browser),
     * which gets AVA from "cold-opened the app and stopped" to "one tap
     * away from the actual video" — a plain launch intent has no way to
     * pick and auto-play a specific video without Google's separate
     * YouTube Data API (its own API key, quota, and a real search call),
     * which is a bigger, separate piece of work than this.
     */
    fun launchYouTubeSearch(query: String): LaunchResult {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            LaunchResult.Success("YouTube")
        } catch (e: Exception) {
            // YouTube app not installed — fall back to opening the same
            // search in whatever browser is available.
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(webIntent)
                LaunchResult.Success("YouTube")
            } catch (e2: Exception) {
                LaunchResult.NotFound("YouTube", emptyList())
            }
        }
    }
}
