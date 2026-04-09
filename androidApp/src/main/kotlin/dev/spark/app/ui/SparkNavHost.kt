package dev.spark.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.spark.app.ui.screen.DashboardScreen
import dev.spark.app.ui.screen.InsightsScreen
import dev.spark.app.ui.screen.OnboardingScreen
import dev.spark.app.ui.screen.SettingsScreen

sealed class SparkDestination(val route: String) {
    data object Onboarding : SparkDestination("onboarding")
    data object Dashboard : SparkDestination("dashboard")
    data object Insights : SparkDestination("insights")
    data object Settings : SparkDestination("settings")
}

@Composable
fun SparkNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SparkDestination.Onboarding.route,
    ) {
        composable(SparkDestination.Onboarding.route) {
            OnboardingScreen(
                onNavigateToDashboard = {
                    navController.navigate(SparkDestination.Dashboard.route) {
                        popUpTo(SparkDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(SparkDestination.Dashboard.route) {
            DashboardScreen(
                onNavigateToInsights = { navController.navigate(SparkDestination.Insights.route) },
                onNavigateToSettings = { navController.navigate(SparkDestination.Settings.route) },
            )
        }
        composable(SparkDestination.Insights.route) {
            InsightsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(SparkDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
