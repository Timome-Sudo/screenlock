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
            ScreenlockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(
                        onboardingDataStore = onboardingDataStore,
                        settingsDataStore = settingsDataStore
                    )
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    onboardingDataStore: OnboardingDataStore,
    settingsDataStore: SettingsDataStore
) {
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isOnboardingCompleted = onboardingDataStore.isOnboardingCompleted.first()
    }

    when (isOnboardingCompleted) {
        null -> {
            // 加载中，显示空白或加载指示器
        }
        true -> {
            MainScreen(settingsDataStore = settingsDataStore)
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
