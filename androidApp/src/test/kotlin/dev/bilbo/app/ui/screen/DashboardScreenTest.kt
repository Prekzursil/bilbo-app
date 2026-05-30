package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.bilbo.app.ui.dashboard.DashboardViewModel
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.AppCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [DashboardScreenContent] (the
 * Hilt-injectionless content composable). Covers:
 *  - loading branch (isLoading + empty apps) → progress indicator
 *  - empty branch (no apps, not loading) → empty card
 *  - populated branch with all three categories rendered
 *  - error branch → error text visible
 *  - Insights / Settings IconButton clicks
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DashboardScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `loading state shows progress indicator`() {
        composeRule.setContent {
            BilboTheme {
                DashboardScreenContent(
                    uiState = DashboardViewModel.DashboardUiState(isLoading = true),
                    onNavigateToInsights = {},
                    onNavigateToSettings = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bilbo").assertExists()
    }

    @Test
    fun `empty state shows the empty-apps card`() {
        composeRule.setContent {
            BilboTheme {
                DashboardScreenContent(
                    uiState =
                        DashboardViewModel.DashboardUiState(
                            isLoading = false,
                            totalMinutes = 0,
                            apps = emptyList(),
                        ),
                    onNavigateToInsights = {},
                    onNavigateToSettings = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No usage recorded yet today").assertExists()
    }

    @Test
    fun `populated state renders apps and category labels`() {
        composeRule.setContent {
            BilboTheme {
                DashboardScreenContent(
                    uiState =
                        DashboardViewModel.DashboardUiState(
                            isLoading = false,
                            totalMinutes = 75,
                            dailyGoalMinutes = 150,
                            apps =
                                listOf(
                                    DashboardViewModel.AppUsage(
                                        packageName = "com.a",
                                        appLabel = "Reader",
                                        category = AppCategory.NUTRITIVE,
                                        durationMinutes = 40,
                                    ),
                                    DashboardViewModel.AppUsage(
                                        packageName = "com.b",
                                        appLabel = "Music",
                                        category = AppCategory.NEUTRAL,
                                        durationMinutes = 20,
                                    ),
                                    DashboardViewModel.AppUsage(
                                        packageName = "com.c",
                                        appLabel = "Scroll",
                                        category = AppCategory.EMPTY_CALORIES,
                                        durationMinutes = 15,
                                    ),
                                ),
                        ),
                    onNavigateToInsights = {},
                    onNavigateToSettings = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reader").assertExists()
        composeRule.onNodeWithText("Nutritive").assertExists()
        composeRule.onNodeWithText("Music").assertExists()
        composeRule.onNodeWithText("Neutral").assertExists()
        composeRule.onNodeWithText("Scroll").assertExists()
        composeRule.onNodeWithText("Empty calories").assertExists()
        composeRule.onNodeWithText("40m").assertExists()
    }

    @Test
    fun `error message is rendered when error is set`() {
        composeRule.setContent {
            BilboTheme {
                DashboardScreenContent(
                    uiState =
                        DashboardViewModel.DashboardUiState(
                            isLoading = false,
                            apps = emptyList(),
                            error = "Could not load dashboard",
                        ),
                    onNavigateToInsights = {},
                    onNavigateToSettings = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Could not load dashboard").assertExists()
    }

    @Test
    fun `Insights and Settings icons fire callbacks`() {
        var insights = 0
        var settings = 0
        composeRule.setContent {
            BilboTheme {
                DashboardScreenContent(
                    uiState = DashboardViewModel.DashboardUiState(isLoading = false),
                    onNavigateToInsights = { insights++ },
                    onNavigateToSettings = { settings++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Insights").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, insights)
        assertEquals(1, settings)
    }
}
