package com.timome.screenlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.timome.screenlock.data.OnboardingDataStore
import com.timome.screenlock.data.SettingsDataStore
import com.timome.screenlock.service.NotificationHelper
import com.timome.screenlock.ui.main.MainScreen
import com.timome.screenlock.ui.main.SettingsRoute
import com.timome.screenlock.ui.onboarding.OnboardingNavHost
import com.timome.screenlock.ui.theme.ScreenlockTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingDataStore = OnboardingDataStore(this)
        val settingsDataStore = SettingsDataStore(this)

        // 初始化通知渠道
        NotificationHelper.createChannels(this)

        setContent {
            AppContent(
                onboardingDataStore = onboardingDataStore,
                settingsDataStore = settingsDataStore
            )
        }
    }
}

@Composable
private fun AppContent(
    onboardingDataStore: OnboardingDataStore,
    settingsDataStore: SettingsDataStore
) {
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

    // Theme settings
    val themeInverted by settingsDataStore.themeInverted.collectAsState(
        initial = SettingsDataStore.DEFAULT_THEME_INVERTED
    )
    val themeDarkMode by settingsDataStore.themeDarkMode.collectAsState(
        initial = SettingsDataStore.DEFAULT_THEME_DARK_MODE
    )

    // Calculate dark theme state based on mode
    val isDarkTheme = when (themeDarkMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme() // "auto"
    }

    LaunchedEffect(Unit) {
        isOnboardingCompleted = onboardingDataStore.isOnboardingCompleted.first()
    }

    ScreenlockTheme(
        darkTheme = isDarkTheme,
        inverted = themeInverted
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (isOnboardingCompleted) {
                null -> {
                    // 加载中
                }
                true -> {
                    MainScreen(
                        settingsDataStore = settingsDataStore
                    )
                }
                false -> {
                    OnboardingNavHost(
                        onboardingDataStore = onboardingDataStore,
                        onOnboardingComplete = {
                            isOnboardingCompleted = true
                        }
                    )
                }
            }
        }
    }
}
