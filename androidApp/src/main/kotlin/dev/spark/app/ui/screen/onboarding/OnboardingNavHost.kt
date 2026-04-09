// OnboardingNavHost.kt
// Spark — Android Onboarding Navigation
//
// Manages the onboarding flow navigation graph.
// Entry point is called from SparkNavHost when onboarding is not completed.

package dev.spark.app.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// MARK: - Route constants

object OnboardingRoute {
    const val WELCOME          = "onboarding/welcome"
    const val PERMISSIONS      = "onboarding/permissions"
    const val APP_CLASSIFICATION = "onboarding/app_classification"
    const val FIRST_INTENT     = "onboarding/first_intent"
}

// MARK: - Nav host

@Composable
fun OnboardingNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onOnboardingComplete: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = OnboardingRoute.WELCOME,
        modifier = modifier
    ) {
        composable(OnboardingRoute.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(OnboardingRoute.PERMISSIONS)
                }
            )
        }

        composable(OnboardingRoute.PERMISSIONS) {
            PermissionsScreen(
                onContinue = {
                    navController.navigate(OnboardingRoute.APP_CLASSIFICATION)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(OnboardingRoute.APP_CLASSIFICATION) {
            AppClassificationScreen(
                onNext = {
                    navController.navigate(OnboardingRoute.FIRST_INTENT)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(OnboardingRoute.FIRST_INTENT) {
            FirstIntentScreen(
                onCompleteSetup = {
                    onOnboardingComplete()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
