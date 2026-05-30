package dev.bilbo.app.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [GatekeeperFPSection].
 *
 * Covers:
 *  - section hidden when isEmptyCalorieApp = false
 *  - section visible when isEmptyCalorieApp = true
 *  - "Your Focus Points" label shown
 *  - estimated cost text displays correctly
 *  - zero balance shows "No Focus Points remaining" red banner
 *  - low balance (1-9) shows "Low balance. N FP remaining." yellow banner
 *  - normal balance (>= 10) shows no banner
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GatekeeperFPSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Visibility (isEmptyCalorieApp) ────────────────────────────────────────

    @Test
    fun `section is not visible when isEmptyCalorieApp is false`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 50,
                    fpEarned = 30,
                    estimatedCostFp = 10,
                    isEmptyCalorieApp = false,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Focus Points").assertDoesNotExist()
    }

    @Test
    fun `section renders when isEmptyCalorieApp is true`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 50,
                    fpEarned = 30,
                    estimatedCostFp = 10,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Focus Points").assertIsDisplayed()
    }

    // ── Cost text ─────────────────────────────────────────────────────────────

    @Test
    fun `estimated cost text shown with correct value`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 40,
                    fpEarned = 20,
                    estimatedCostFp = 15,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This will cost ~15 FP").assertIsDisplayed()
    }

    // ── Zero balance banner ───────────────────────────────────────────────────

    @Test
    fun `zero balance shows No Focus Points remaining banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 0,
                    fpEarned = 0,
                    estimatedCostFp = 5,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Focus Points remaining. Earn more first.").assertIsDisplayed()
    }

    @Test
    fun `negative balance shows No Focus Points remaining banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = -5,
                    fpEarned = 0,
                    estimatedCostFp = 5,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Focus Points remaining. Earn more first.").assertIsDisplayed()
    }

    // ── Low balance banner ────────────────────────────────────────────────────

    @Test
    fun `low balance (1) shows Low balance banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 1,
                    fpEarned = 5,
                    estimatedCostFp = 5,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Low balance. 1 FP remaining.").assertIsDisplayed()
    }

    @Test
    fun `low balance (9) shows Low balance banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 9,
                    fpEarned = 10,
                    estimatedCostFp = 5,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Low balance. 9 FP remaining.").assertIsDisplayed()
    }

    // ── Normal balance: no banner ─────────────────────────────────────────────

    @Test
    fun `balance at threshold (10) shows no banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 10,
                    fpEarned = 10,
                    estimatedCostFp = 5,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Focus Points remaining", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Low balance", substring = true).assertDoesNotExist()
    }

    @Test
    fun `high balance shows no banner`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 100,
                    fpEarned = 60,
                    estimatedCostFp = 20,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No Focus Points remaining", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Low balance", substring = true).assertDoesNotExist()
    }

    // ── renders without crash with default modifier ───────────────────────────

    @Test
    fun `renders with zero cost without crash`() {
        composeRule.setContent {
            BilboTheme {
                GatekeeperFPSection(
                    currentBalance = 50,
                    fpEarned = 30,
                    estimatedCostFp = 0,
                    isEmptyCalorieApp = true,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This will cost ~0 FP").assertIsDisplayed()
    }
}
