// BilboNavHost.kt
// Bilbo — Android Navigation Host
//
// Root navigation graph with:
//   • Conditional onboarding flow (first launch)
//   • Bottom navigation bar (5 tabs)
//   • Full deep-link route graph for all screens
//
// Top-level tab screens are wired to their real composables; deep sub-routes
// (Budget, Challenges, Circles, Buddies, etc.) render with default ui state
// until the corresponding ViewModels are implemented.

package dev.bilbo.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bilbo.app.ui.screen.AnalogSuggestionsScreen
import dev.bilbo.app.ui.screen.AnalogSuggestionsUiState
import dev.bilbo.app.ui.screen.BudgetDashboardScreen
import dev.bilbo.app.ui.screen.BudgetDashboardUiState
import dev.bilbo.app.ui.screen.ChallengeScreen
import dev.bilbo.app.ui.screen.CircleScreen
import dev.bilbo.app.ui.screen.DashboardScreen
import dev.bilbo.app.ui.screen.DataAnonymizationScreen
import dev.bilbo.app.ui.screen.DigestScreen
import dev.bilbo.app.ui.screen.InsightsScreen
import dev.bilbo.app.ui.screen.LeaderboardScreen
import dev.bilbo.app.ui.screen.SocialHubScreen
import dev.bilbo.app.ui.screen.WeeklyInsightScreen
import dev.bilbo.app.ui.screen.onboarding.OnboardingNavHost
import dev.bilbo.app.ui.screen.settings.SettingsScreen as RealSettingsScreen

// MARK: - Top-level route constants

object BilboRoute {
    // Top-level tabs
    const val DASHBOARD = "dashboard"
    const val FOCUS = "focus"
    const val INSIGHTS = "insights"
    const val SOCIAL = "social"
    const val SETTINGS = "settings"

    // Dashboard sub-routes
    const val BUDGET = "budget"

    // Insights sub-routes
    const val WEEKLY_INSIGHT = "insights/weekly/{weekStart}"
    const val ANALOG_SUGGESTIONS = "analog/suggestions"
    const val INTERESTS_ONBOARDING = "interests/setup"

    // Social sub-routes
    const val SOCIAL_HUB = "social/hub"
    const val BUDDY_PAIRS = "social/buddies"
    const val CIRCLES = "social/circles"
    const val CHALLENGES = "social/challenges"
    const val LEADERBOARD = "social/leaderboard"
    const val DIGEST = "social/digest"

    // Settings sub-routes
    const val SETTINGS_ENFORCEMENT = "settings/enforcement"
    const val SETTINGS_ECONOMY = "settings/economy"
    const val SETTINGS_EMOTIONAL = "settings/emotional"
    const val SETTINGS_AI = "settings/ai"
    const val SETTINGS_SOCIAL = "settings/social"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_DATA = "settings/data"
    const val DATA_ANONYMIZATION = "settings/data/anonymization"

    // Onboarding (full-screen, no bottom bar)
    const val ONBOARDING = "onboarding"
}

// MARK: - Bottom navigation items

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val bottomNavItems = listOf(
    BottomNavItem(BilboRoute.DASHBOARD, "Dashboard", Icons.Filled.GridView),
    BottomNavItem(BilboRoute.FOCUS, "Focus", Icons.Filled.Shield),
    BottomNavItem(BilboRoute.INSIGHTS, "Insights", Icons.Filled.BarChart),
    BottomNavItem(BilboRoute.SOCIAL, "Social", Icons.Filled.People),
    BottomNavItem(BilboRoute.SETTINGS, "Settings", Icons.Filled.Settings),
)

// Routes that do NOT show the bottom bar
private val fullScreenRoutes = setOf(BilboRoute.ONBOARDING)

// MARK: - Root NavHost

@Composable
fun BilboNavHost(
    onboardingCompleted: Boolean,
    onOnboardingFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute?.let { route ->
        fullScreenRoutes.none { route.startsWith(it) }
    } ?: true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                BilboBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) BilboRoute.DASHBOARD else BilboRoute.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } },
        ) {

            // ── Onboarding ───────────────────────────────────────────────
            composable(BilboRoute.ONBOARDING) {
                OnboardingNavHost(
                    onOnboardingComplete = {
                        onOnboardingFinished()
                        navController.navigate(BilboRoute.DASHBOARD) {
                            popUpTo(BilboRoute.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            // ── Dashboard ────────────────────────────────────────────────
            composable(BilboRoute.DASHBOARD) {
                DashboardScreen(
                    onNavigateToInsights = { navController.navigate(BilboRoute.INSIGHTS) },
                    onNavigateToSettings = { navController.navigate(BilboRoute.SETTINGS) },
                )
            }

            composable(BilboRoute.BUDGET) {
                BudgetDashboardScreen(
                    uiState = BudgetDashboardUiState(),
                    onRefresh = {},
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Focus ────────────────────────────────────────────────────
            composable(BilboRoute.FOCUS) {
                FocusPlaceholder()
            }

            // ── Insights ─────────────────────────────────────────────────
            composable(BilboRoute.INSIGHTS) {
                InsightsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = BilboRoute.WEEKLY_INSIGHT,
                arguments = listOf(navArgument("weekStart") { type = NavType.StringType }),
            ) {
                WeeklyInsightScreen(
                    insight = null,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(BilboRoute.ANALOG_SUGGESTIONS) {
                AnalogSuggestionsScreen(
                    uiState = AnalogSuggestionsUiState(),
                    onAccept = {},
                    onShowAnother = {},
                    onAddCustom = {},
                    onDeleteCustom = {},
                    onBack = { navController.popBackStack() },
                )
            }

            composable(BilboRoute.INTERESTS_ONBOARDING) {
                // Full interests onboarding flow is exposed via the onboarding module;
                // tapping "Edit interests" from Settings routes back to Dashboard.
                InterestsReconfigurePlaceholder(onDone = { navController.popBackStack() })
            }

            // ── Social ───────────────────────────────────────────────────
            composable(BilboRoute.SOCIAL) {
                SocialHubScreen(
                    onBuddyPairTap = { _ -> navController.navigate(BilboRoute.BUDDY_PAIRS) },
                    onCircleTap = { _ -> navController.navigate(BilboRoute.CIRCLES) },
                    onChallengeTap = { _ -> navController.navigate(BilboRoute.CHALLENGES) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(BilboRoute.BUDDY_PAIRS) {
                dev.bilbo.app.ui.screen.BuddyPairScreen(onBack = { navController.popBackStack() })
            }
            composable(BilboRoute.CIRCLES) {
                CircleScreen(onBack = { navController.popBackStack() })
            }
            composable(BilboRoute.CHALLENGES) {
                ChallengeScreen(onBack = { navController.popBackStack() })
            }
            composable(BilboRoute.LEADERBOARD) {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }
            composable(BilboRoute.DIGEST) {
                DigestScreen(onDismiss = { navController.popBackStack() })
            }

            // ── Settings ─────────────────────────────────────────────────
            composable(BilboRoute.SETTINGS) {
                RealSettingsScreen()
            }
            composable(BilboRoute.SETTINGS_ENFORCEMENT) { SettingsSubPlaceholder("Enforcement") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_ECONOMY) { SettingsSubPlaceholder("Focus Economy") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_EMOTIONAL) { SettingsSubPlaceholder("Emotional Check-ins") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_AI) { SettingsSubPlaceholder("AI Insights") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_SOCIAL) { SettingsSubPlaceholder("Social") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_NOTIFICATIONS) { SettingsSubPlaceholder("Notifications") { navController.popBackStack() } }
            composable(BilboRoute.SETTINGS_DATA) { SettingsSubPlaceholder("Data & Privacy") { navController.popBackStack() } }
            composable(BilboRoute.DATA_ANONYMIZATION) {
                DataAnonymizationScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// MARK: - Bottom Navigation Bar

@Composable
private fun BilboBottomBar(
    navController: NavController,
    currentRoute: String?,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

// MARK: - Helpers

@Composable
private fun FocusPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Focus sessions start from outside the app — open a tracked app " +
                "and Bilbo will intercept with the Intent Gatekeeper.",
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun InterestsReconfigurePlaceholder(onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Interests can be re-selected during onboarding.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSubPlaceholder(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("$title settings coming soon")
        }
    }
}
