package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [BudgetDashboardScreen].
 *
 * The screen uses PullToRefreshBox (ExperimentalMaterial3Api) and a Scaffold
 * with TopAppBar.  The back navigation icon is only shown when `onBack != null`.
 *
 * Covers:
 *  - TopAppBar title "Focus Economy" rendered
 *  - Back button shown when onBack is non-null
 *  - Back button fires onBack callback
 *  - Back button absent when onBack is null
 *  - All section card titles rendered (Today's Activity, This Week, Time Breakdown, Streak)
 *  - TodayActivitySection: earned/spent/bonus text line
 *  - StreakSection: streak days text rendered
 *  - StreakSection: 7-day bonus text shown when streakDays >= 7 (conditional branch)
 *  - StreakSection: 7-day bonus text NOT shown when streakDays < 7
 *  - TimeBreakdownSection: Nutritive / Neutral / Empty Calories bars with minute values
 *  - isRefreshing = false renders normally (no infinite spinner blocking assertions)
 *  - zero streak: "0 day streak" rendered
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetDashboardScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        uiState: BudgetDashboardUiState = BudgetDashboardUiState(),
        onRefresh: () -> Unit = {},
        onBack: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            BilboTheme {
                BudgetDashboardScreen(
                    uiState = uiState,
                    onRefresh = onRefresh,
                    onBack = onBack,
                )
            }
        }
        composeRule.waitForIdle()
    }

    // ── TopAppBar ─────────────────────────────────────────────────────────────

    @Test
    fun `TopAppBar title Focus Economy is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Focus Economy").assertExists()
    }

    @Test
    fun `Back button shown when onBack is non-null`() {
        setScreen(onBack = {})
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun `Back button fires onBack callback`() {
        var backCount = 0
        setScreen(onBack = { backCount++ })
        composeRule.onNodeWithContentDescription("Back")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `Back button absent when onBack is null`() {
        setScreen(onBack = null)
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    // ── Section card titles ───────────────────────────────────────────────────

    @Test
    fun `Today's Activity section card is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Today's Activity").assertExists()
    }

    @Test
    fun `This Week section card is rendered`() {
        setScreen()
        composeRule.onNodeWithText("This Week").assertExists()
    }

    @Test
    fun `Time Breakdown section card is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Time Breakdown").assertExists()
    }

    @Test
    fun `Streak section card is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Streak").assertExists()
    }

    // ── TodayActivitySection ──────────────────────────────────────────────────

    @Test
    fun `TodayActivity earned spent bonus summary text is shown`() {
        setScreen(
            uiState = BudgetDashboardUiState(fpEarned = 30, fpSpent = 10, fpBonus = 5),
        )
        composeRule.onNodeWithText("+30 earned  |  -10 spent  |  +5 bonus").assertExists()
    }

    @Test
    fun `TodayActivity with zero values shows zeros`() {
        setScreen(
            uiState = BudgetDashboardUiState(fpEarned = 0, fpSpent = 0, fpBonus = 0),
        )
        composeRule.onNodeWithText("+0 earned  |  -0 spent  |  +0 bonus").assertExists()
    }

    // ── StreakSection ─────────────────────────────────────────────────────────

    @Test
    fun `streak days text is rendered`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 3))
        composeRule.onNodeWithText("3 day streak").assertExists()
    }

    @Test
    fun `zero streak renders 0 day streak`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 0))
        composeRule.onNodeWithText("0 day streak").assertExists()
    }

    @Test
    fun `seven-day bonus text shown when streakDays is 7 (STREAK_BONUS_THRESHOLD)`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 7))
        // StreakSection: if (streakDays >= STREAK_BONUS_THRESHOLD) → shows bonus text
        composeRule.onNodeWithText("7-day bonus: +20 FP").assertExists()
    }

    @Test
    fun `seven-day bonus text shown when streakDays exceeds threshold`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 14))
        composeRule.onNodeWithText("7-day bonus: +20 FP").assertExists()
    }

    @Test
    fun `seven-day bonus text NOT shown when streakDays below threshold`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 6))
        composeRule.onNodeWithText("7-day bonus: +20 FP").assertDoesNotExist()
    }

    @Test
    fun `seven-day bonus text NOT shown when streakDays is zero`() {
        setScreen(uiState = BudgetDashboardUiState(streakDays = 0))
        composeRule.onNodeWithText("7-day bonus: +20 FP").assertDoesNotExist()
    }

    // ── TimeBreakdownSection ──────────────────────────────────────────────────

    @Test
    fun `TimeBreakdown Nutritive label is shown`() {
        setScreen()
        composeRule.onNodeWithText("Nutritive").assertExists()
    }

    @Test
    fun `TimeBreakdown Neutral label is shown`() {
        setScreen()
        composeRule.onNodeWithText("Neutral").assertExists()
    }

    @Test
    fun `TimeBreakdown Empty Calories label is shown`() {
        setScreen()
        composeRule.onNodeWithText("Empty Calories").assertExists()
    }

    @Test
    fun `TimeBreakdown shows minute values`() {
        setScreen(
            uiState = BudgetDashboardUiState(
                nutritiveMinutes = 60,
                neutralMinutes = 30,
                emptyCalorieMinutes = 120,
            ),
        )
        composeRule.onNodeWithText("60m").assertExists()
        composeRule.onNodeWithText("30m").assertExists()
        composeRule.onNodeWithText("120m").assertExists()
    }

    @Test
    fun `TimeBreakdown zero minutes shows 0m for all bars`() {
        setScreen(
            uiState = BudgetDashboardUiState(
                nutritiveMinutes = 0,
                neutralMinutes = 0,
                emptyCalorieMinutes = 0,
            ),
        )
        // Three "0m" labels expected (one per bar) — use onFirst to tolerate multiple
        composeRule.onAllNodesWithText("0m").onFirst().assertExists()
    }

    // ── isRefreshing state ────────────────────────────────────────────────────

    @Test
    fun `screen renders normally when isRefreshing is false`() {
        setScreen(uiState = BudgetDashboardUiState(isRefreshing = false))
        composeRule.onNodeWithText("Focus Economy").assertExists()
    }

    // ── WeeklyChart ───────────────────────────────────────────────────────────

    @Test
    fun `weekly balances with all zeros renders without crash`() {
        setScreen(
            uiState = BudgetDashboardUiState(weeklyBalances = List(7) { 0 }),
        )
        composeRule.onNodeWithText("This Week").assertExists()
    }

    @Test
    fun `weekly balances with varying values renders without crash`() {
        setScreen(
            uiState = BudgetDashboardUiState(weeklyBalances = listOf(10, 20, 15, 30, 5, 40, 25)),
        )
        composeRule.onNodeWithText("This Week").assertExists()
    }

    @Test
    fun `weekly balances shorter than 7 renders without crash`() {
        setScreen(
            uiState = BudgetDashboardUiState(weeklyBalances = listOf(5, 10, 15)),
        )
        composeRule.onNodeWithText("This Week").assertExists()
    }

    // ── Full state integration ────────────────────────────────────────────────

    @Test
    fun `full dashboard renders all sections with real data without crash`() {
        setScreen(
            uiState = BudgetDashboardUiState(
                currentBalance = 150,
                fpEarned = 50,
                fpSpent = 20,
                fpBonus = 10,
                nutritiveMinutes = 45,
                neutralMinutes = 30,
                emptyCalorieMinutes = 90,
                streakDays = 8,
                weeklyBalances = listOf(100, 120, 90, 150, 80, 200, 130),
                isRefreshing = false,
            ),
            onBack = {},
        )
        composeRule.onNodeWithText("Focus Economy").assertExists()
        composeRule.onNodeWithText("Today's Activity").assertExists()
        composeRule.onNodeWithText("8 day streak").assertExists()
        composeRule.onNodeWithText("7-day bonus: +20 FP").assertExists()
    }
}
