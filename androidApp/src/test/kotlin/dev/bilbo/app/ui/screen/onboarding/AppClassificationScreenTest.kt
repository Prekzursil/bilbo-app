package dev.bilbo.app.ui.screen.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [AppClassificationScreen].
 *
 * Covers:
 *  - header / title text rendered
 *  - classification legend chips (Nutritive, Neutral, Empty Calories) shown
 *  - "Looks Good!" button fires onNext callback
 *  - back button fires onBack callback
 *  - screen composes without crash (app list may be empty in Robolectric environment)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppClassificationScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `header title and subtitle are rendered`() {
        composeRule.setContent {
            BilboTheme {
                AppClassificationScreen(onNext = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Classify Your Apps").assertExists()
        composeRule.onNodeWithText("How should we categorize your most-used apps?").assertExists()
    }

    @Test
    fun `classification legend chips are all shown`() {
        composeRule.setContent {
            BilboTheme {
                AppClassificationScreen(onNext = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        // Legend uses the enum labels; each appears at least once (legend row + possibly app rows)
        composeRule.onAllNodesWithText(AppClassification.NUTRITIVE.label).onFirst().assertExists()
        composeRule.onAllNodesWithText(AppClassification.NEUTRAL.label).onFirst().assertExists()
        composeRule.onAllNodesWithText(AppClassification.EMPTY_CALORIES.label).onFirst().assertExists()
    }

    @Test
    fun `tapping Looks Good fires onNext`() {
        var nextCount = 0
        composeRule.setContent {
            BilboTheme {
                AppClassificationScreen(onNext = { nextCount++ }, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Looks Good!").performClick()
        composeRule.waitForIdle()
        assertEquals(1, nextCount)
    }

    @Test
    fun `back button fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                AppClassificationScreen(onNext = {}, onBack = { backCount++ })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Classify Your Apps").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `screen composes without crash with no installed apps`() {
        // In a Robolectric environment PackageManager may return 0 non-system apps;
        // the screen must still render header + legend + button.
        composeRule.setContent {
            BilboTheme {
                AppClassificationScreen(onNext = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Looks Good!").assertExists()
    }
}
