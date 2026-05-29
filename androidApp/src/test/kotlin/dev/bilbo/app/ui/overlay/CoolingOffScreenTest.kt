package dev.bilbo.app.ui.overlay

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
 * Robolectric Compose UI test for [CoolingOffScreen]. The screen kicks off a
 * 10-second breathing animation as a `LaunchedEffect`; we render it and verify
 * the initial frame is composed with the expected static labels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CoolingOffScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders without crashing and shows FP-on-completion footer`() {
        composeRule.setContent {
            BilboTheme {
                CoolingOffScreen(onComplete = {})
            }
        }
        composeRule.waitForIdle()
        // The breathing text changes through phases; the FP footer is static.
        composeRule.onNodeWithText("+3 FP on completion").assertExists()
    }
}
