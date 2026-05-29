package dev.bilbo.app.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI test for [FPBalanceWidget]. Exercises every colour
 * branch of the balance threshold (high → green, mid → yellow, low → red)
 * and the progress-ring fill clamp (negative + over-cap).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FPBalanceWidgetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders balance number and label - high green branch`() {
        composeRule.setContent {
            BilboTheme { FPBalanceWidget(currentBalance = 50, fpEarned = 30) }
        }
        composeRule.onNodeWithText("50").assertExists()
        composeRule.onNodeWithText("Focus Points").assertExists()
    }

    @Test
    fun `renders mid-range yellow branch`() {
        composeRule.setContent {
            BilboTheme { FPBalanceWidget(currentBalance = 20, fpEarned = 15) }
        }
        composeRule.onNodeWithText("20").assertExists()
    }

    @Test
    fun `renders low red branch with zero earned`() {
        composeRule.setContent {
            BilboTheme { FPBalanceWidget(currentBalance = 5, fpEarned = 0) }
        }
        composeRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun `renders negative balance triggers red branch`() {
        composeRule.setContent {
            BilboTheme { FPBalanceWidget(currentBalance = -3, fpEarned = -10) }
        }
        composeRule.onNodeWithText("-3").assertExists()
    }

    @Test
    fun `over-cap earned coerces to full ring`() {
        composeRule.setContent {
            BilboTheme { FPBalanceWidget(currentBalance = 100, fpEarned = 9999) }
        }
        composeRule.onNodeWithText("100").assertExists()
    }

    @Test
    fun `renders at the boundary value 30 (yellow branch)`() {
        composeRule.setContent {
            // currentBalance == HIGH_THRESHOLD (30) takes the yellow branch (strict >).
            BilboTheme { FPBalanceWidget(currentBalance = 30, fpEarned = 30) }
        }
        composeRule.onNodeWithText("30").assertExists()
    }

    @Test
    fun `renders at the boundary value 10 (red branch)`() {
        composeRule.setContent {
            // currentBalance == LOW_THRESHOLD (10) — strict-> goes to red.
            BilboTheme { FPBalanceWidget(currentBalance = 10, fpEarned = 10) }
        }
        composeRule.onNodeWithText("10").assertExists()
    }
}
