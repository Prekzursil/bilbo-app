package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI test for [OnboardingScreen]. Exercises the rendered
 * tagline + both call-to-action buttons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders brand heading and tagline`() {
        composeRule.setContent {
            OnboardingScreen(onNavigateToDashboard = {})
        }
        composeRule.onNodeWithText("Bilbo").assertExists()
        composeRule
            .onNodeWithText(
                "Your digital wellness companion.\nBuild mindful habits, one screen at a time.",
            ).assertExists()
    }

    @Test
    fun `Get Started button invokes the navigate callback`() {
        var calls = 0
        composeRule.setContent {
            OnboardingScreen(onNavigateToDashboard = { calls++ })
        }
        composeRule.onNodeWithText("Get Started").performClick()
        assertEquals(1, calls)
    }

    @Test
    fun `Sign In button invokes the navigate callback`() {
        var calls = 0
        composeRule.setContent {
            OnboardingScreen(onNavigateToDashboard = { calls++ })
        }
        composeRule.onNodeWithText("Sign In").performClick()
        assertEquals(1, calls)
    }
}
