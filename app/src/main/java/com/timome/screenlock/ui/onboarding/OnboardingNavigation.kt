package com.timome.screenlock.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.timome.screenlock.data.OnboardingDataStore
import kotlinx.coroutines.launch

sealed class OnboardingScreen(val route: String) {
    data object Welcome : OnboardingScreen("welcome")
    data object Terms : OnboardingScreen("terms")
    data object Permissions : OnboardingScreen("permissions")
}

@Composable
fun OnboardingNavHost(
    onboardingDataStore: OnboardingDataStore,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = OnboardingScreen.Welcome.route,
        modifier = modifier
    ) {
        composable(OnboardingScreen.Welcome.route) {
            WelcomeScreen(
                onNextClick = {
                    navController.navigate(OnboardingScreen.Terms.route)
                }
            )
        }

        composable(OnboardingScreen.Terms.route) {
            TermsScreen(
                onNextClick = {
                    navController.navigate(OnboardingScreen.Permissions.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(OnboardingScreen.Permissions.route) {
            PermissionsScreen(
                onNextClick = {
                    scope.launch {
                        onboardingDataStore.setOnboardingCompleted(true)
                        onOnboardingComplete()
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
