package com.fahimkhan.ava.core.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * Finds the best-matching installed app for a spoken name and launches it.
     * Matching is tolerant: exact label match wins, then "contains" in either
     * direction, so "ইউটিউব" matches "YouTube" is NOT possible by text alone —
     * callers should pass the Latin/English app name where possible; common
     * Bengali app nicknames are normalized by the caller before this is invoked.
     */
    fun launchApp(spokenName: String): LaunchResult {
        val query = spokenName.trim().lowercase()
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
}
