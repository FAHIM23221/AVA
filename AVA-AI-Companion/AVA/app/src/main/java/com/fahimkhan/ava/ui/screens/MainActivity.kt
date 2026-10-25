package com.fahimkhan.ava.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fahimkhan.ava.ui.theme.AvaColors
import com.fahimkhan.ava.ui.theme.AvaTheme
import dagger.hilt.android.AndroidEntryPoint

object AvaRoutes {
    const val SPLASH   = "splash"
    const val MAIN     = "main"
    const val ADMIN    = "admin"
    const val MEMORY   = "memory"
    const val SETTINGS = "settings"
    const val VISION   = "vision"
    const val SECURITY = "security"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { mainViewModel.isLoading.value }
        super.onCreate(savedInstanceState)
        handleWakeWordLaunch(intent)
        setContent {
            AvaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AvaColors.BlackAbyss) {
                    AvaNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeWordLaunch(intent)
    }

    /**
     * AvaForegroundService launches/brings forward this Activity with
     * FLAG_ACTIVITY_SINGLE_TOP when the wake word fires, which means an
     * already-running Activity gets onNewIntent() instead of onCreate()
     * — both paths are handled here so "AVA" reliably starts listening
     * immediately instead of requiring a manual tap afterward.
     */
    private fun handleWakeWordLaunch(intent: Intent?) {
        if (intent?.getBooleanExtra("WAKE_WORD_TRIGGERED", false) == true) {
            mainViewModel.startListeningFromWakeWord()
        }
    }
}

@Composable
fun AvaNavGraph() {
    val navController: NavHostController = rememberNavController()
    NavHost(navController = navController, startDestination = AvaRoutes.SPLASH) {
        composable(AvaRoutes.SPLASH)    { SplashScreen(onComplete = { navController.navigate(AvaRoutes.MAIN) { popUpTo(AvaRoutes.SPLASH) { inclusive = true } } }) }
        composable(AvaRoutes.MAIN)      { MainScreen(onNavigateAdmin = { navController.navigate(AvaRoutes.ADMIN) }, onNavigateMemory = { navController.navigate(AvaRoutes.MEMORY) }, onNavigateSettings = { navController.navigate(AvaRoutes.SETTINGS) }, onNavigateVision = { navController.navigate(AvaRoutes.VISION) }, onNavigateSecurity = { navController.navigate(AvaRoutes.SECURITY) }) }
        composable(AvaRoutes.ADMIN)     { AdminScreen(onBack = { navController.popBackStack() }) }
        composable(AvaRoutes.MEMORY)    { MemoryScreen(onBack = { navController.popBackStack() }) }
        composable(AvaRoutes.SETTINGS)  { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(AvaRoutes.VISION)    { VisionScreen(onBack = { navController.popBackStack() }) }
        composable(AvaRoutes.SECURITY)  { SecurityScreen(onBack = { navController.popBackStack() }) }
    }
}
