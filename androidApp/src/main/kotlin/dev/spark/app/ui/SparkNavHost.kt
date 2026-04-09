// SparkNavHost.kt
// Spark — Android Navigation Host
//
// Root navigation graph with:
//   • Conditional onboarding flow (first launch)
//   • Bottom navigation bar (5 tabs)
//   • Full deep-link route graph for all screens

package dev.spark.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.compose.*
import dev.spark.app.ui.screen.onboarding.OnboardingNavHost
import dev.spark.app.ui.screen.onboarding.OnboardingRoute
import dev.spark.app.ui.screen.settings.SettingsScreen

// MARK: - Top-level route constants

object SparkRoute {
    // Top-level tabs
    const val DASHBOARD         = "dashboard"
    const val FOCUS             = "focus"
    const val INSIGHTS          = "insights"
    const val SOCIAL            = "social"
    const val SETTINGS          = "settings"

    // Dashboard sub-routes
    const val BUDGET            = "budget"

    // Insights sub-routes
    const val WEEKLY_INSIGHT    = "insights/weekly/{weekStart}"
    const val ANALOG_SUGGESTIONS = "analog/suggestions"
    const val INTERESTS_ONBOARDING = "interests/setup"

    // Social sub-routes
    const val SOCIAL_HUB        = "social/hub"
    const val BUDDY_PAIRS       = "social/buddies"
    const val CIRCLES           = "social/circles"
    const val CHALLENGES        = "social/challenges"
    const val LEADERBOARD       = "social/leaderboard"
    const val DIGEST            = "social/digest"

    // Settings sub-routes
    const val SETTINGS_ENFORCEMENT  = "settings/enforcement"
    const val SETTINGS_ECONOMY      = "settings/economy"
    const val SETTINGS_EMOTIONAL    = "settings/emotional"
    const val SETTINGS_AI           = "settings/ai"
    const val SETTINGS_SOCIAL       = "settings/social"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_DATA         = "settings/data"
    const val DATA_ANONYMIZATION    = "settings/data/anonymization"

    // Onboarding (full-screen, no bottom bar)
    const val ONBOARDING        = "onboarding"
}

// MARK: - Bottom navigation items

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

private val bottomNavItems = listOf(
    BottomNavItem(SparkRoute.DASHBOARD, "Dashboard", Icons.Filled.GridView),
    BottomNavItem(SparkRoute.FOCUS,     "Focus",     Icons.Filled.Shield),
    BottomNavItem(SparkRoute.INSIGHTS,  "Insights",  Icons.Filled.BarChart),
    BottomNavItem(SparkRoute.SOCIAL,    "Social",    Icons.Filled.People),
    BottomNavItem(SparkRoute.SETTINGS,  "Settings",  Icons.Filled.Settings)
)

// Routes that do NOT show the bottom bar
private val fullScreenRoutes = setOf(SparkRoute.ONBOARDING)

// MARK: - Root NavHost

@Composable
fun SparkNavHost(
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
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
                SparkBottomBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) SparkRoute.DASHBOARD else SparkRoute.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } }
        ) {

            // ── Onboarding ───────────────────────────────────────────────
            composable(SparkRoute.ONBOARDING) {
                OnboardingNavHost(
                    onOnboardingComplete = {
                        navController.navigate(SparkRoute.DASHBOARD) {
                            popUpTo(SparkRoute.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // ── Dashboard ────────────────────────────────────────────────
            composable(SparkRoute.DASHBOARD) {
                DashboardPlaceholder(
                    onBudgetClick = { navController.navigate(SparkRoute.BUDGET) },
                    onSuggestionsClick = { navController.navigate(SparkRoute.ANALOG_SUGGESTIONS) }
                )
            }

            composable(SparkRoute.BUDGET) {
                ScreenPlaceholder("Budget") { navController.popBackStack() }
            }

            // ── Focus ────────────────────────────────────────────────────
            composable(SparkRoute.FOCUS) {
                ScreenPlaceholder("Focus / Gatekeeper")
            }

            // ── Insights ─────────────────────────────────────────────────
            composable(SparkRoute.INSIGHTS) {
                InsightsPlaceholder(
                    onWeeklyInsightClick = { weekStart ->
                        navController.navigate("insights/weekly/$weekStart")
                    }
                )
            }

            composable(
                route = SparkRoute.WEEKLY_INSIGHT,
                arguments = listOf(navArgument("weekStart") { type = NavType.StringType })
            ) { backStack ->
                val weekStart = backStack.arguments?.getString("weekStart") ?: ""
                ScreenPlaceholder("Weekly Insight: $weekStart") { navController.popBackStack() }
            }

            composable(SparkRoute.ANALOG_SUGGESTIONS) {
                ScreenPlaceholder("Analog Suggestions") { navController.popBackStack() }
            }

            composable(SparkRoute.INTERESTS_ONBOARDING) {
                ScreenPlaceholder("My Interests") { navController.popBackStack() }
            }

            // ── Social ───────────────────────────────────────────────────
            composable(SparkRoute.SOCIAL) {
                SocialHubPlaceholder(navController = navController)
            }
            composable(SparkRoute.BUDDY_PAIRS)   { ScreenPlaceholder("Accountability Buddies") { navController.popBackStack() } }
            composable(SparkRoute.CIRCLES)        { ScreenPlaceholder("Circles") { navController.popBackStack() } }
            composable(SparkRoute.CHALLENGES)     { ScreenPlaceholder("Challenges") { navController.popBackStack() } }
            composable(SparkRoute.LEADERBOARD)    { ScreenPlaceholder("Leaderboard") { navController.popBackStack() } }
            composable(SparkRoute.DIGEST)         { ScreenPlaceholder("Weekly Digest") { navController.popBackStack() } }

            // ── Settings ─────────────────────────────────────────────────
            composable(SparkRoute.SETTINGS) {
                SettingsScreen()
            }
            composable(SparkRoute.SETTINGS_ENFORCEMENT)   { ScreenPlaceholder("Enforcement Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_ECONOMY)        { ScreenPlaceholder("Economy Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_EMOTIONAL)      { ScreenPlaceholder("Emotional Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_AI)             { ScreenPlaceholder("AI Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_SOCIAL)         { ScreenPlaceholder("Social Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_NOTIFICATIONS)  { ScreenPlaceholder("Notification Settings") { navController.popBackStack() } }
            composable(SparkRoute.SETTINGS_DATA)           { ScreenPlaceholder("Data Settings") { navController.popBackStack() } }
            composable(SparkRoute.DATA_ANONYMIZATION)      { ScreenPlaceholder("Data Anonymization Preview") { navController.popBackStack() } }
        }
    }
}

// MARK: - Bottom Navigation Bar

@Composable
private fun SparkBottomBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label
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
                }
            )
        }
    }
}

// MARK: - Screen placeholder composables (replaced by real screens in production)

@Composable
private fun DashboardPlaceholder(
    onBudgetClick: () -> Unit,
    onSuggestionsClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Dashboard — coming soon")
    }
}

@Composable
private fun InsightsPlaceholder(onWeeklyInsightClick: (String) -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Insights — coming soon")
    }
}

@Composable
private fun SocialHubPlaceholder(navController: NavController) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Social Hub — coming soon")
    }
}

@Composable
private fun ScreenPlaceholder(title: String, onBack: (() -> Unit)? = null) {
    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(title)
        }
    }
}

// Import needed at call site
private fun Modifier.fillMaxSize() = this.then(androidx.compose.foundation.layout.fillMaxSize())
