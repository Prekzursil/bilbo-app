package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI test for [InsightsScreen]. Renders the screen and
 * exercises both the title/body text branch and the back-button click branch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InsightsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders title and body`() {
        composeRule.setContent {
            InsightsScreen(onNavigateBack = {})
        }
        composeRule.onNodeWithText("Insights").assertExists()
        composeRule.onNodeWithText("AI insights coming soon").assertExists()
    }

    @Test
    fun `back button invokes the navigate-back callback`() {
        var called = 0
        composeRule.setContent {
            InsightsScreen(onNavigateBack = { called++ })
        }
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(called == 1)
    }
}
