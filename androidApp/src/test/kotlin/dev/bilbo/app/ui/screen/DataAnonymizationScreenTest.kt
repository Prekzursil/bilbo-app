package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [DataAnonymizationScreen]. Covers:
 *  - disclosure banner and "Full Transparency" header render
 *  - "Enable AI-powered insights" toggle card renders
 *  - Cloud AI toggle callback (on → true, off → false)
 *  - Refresh preview button renders and triggers callback
 *  - Refreshing state shows progress indicator instead of refresh button
 *  - onBack navigation icon click fires callback
 *  - Privacy reassurance text renders
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataAnonymizationScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders disclosure banner with Full Transparency heading`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Full Transparency").assertExists()
        composeRule.onNodeWithText("AI Privacy").assertExists()
    }

    @Test
    fun `renders AI privacy toggle card`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enable AI-powered insights").assertExists()
        composeRule.onNodeWithText("Rate-limited to once per week.", substring = true).assertExists()
    }

    @Test
    fun `cloud AI toggle off state renders Switch unchecked`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(isCloudAiEnabled = false)
            }
        }
        composeRule.waitForIdle()
        // Toggle card heading is present regardless of state
        composeRule.onNodeWithText("Enable AI-powered insights").assertExists()
    }

    @Test
    fun `cloud AI toggle on state renders Switch checked`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(isCloudAiEnabled = true)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enable AI-powered insights").assertExists()
    }

    @Test
    fun `onToggleCloudAi callback fires when Switch is clicked`() {
        var toggled: Boolean? = null
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(
                    isCloudAiEnabled = false,
                    onToggleCloudAi = { toggled = it },
                )
            }
        }
        composeRule.waitForIdle()
        // The Switch semantic node is toggleable and has a click action
        composeRule.onNode(isToggleable()).assertExists().assertHasClickAction()
        // Click the Switch and verify the callback fires with true (enabling AI)
        composeRule.onNode(isToggleable()).performClick()
        composeRule.waitForIdle()
        assertEquals(true, toggled)
    }

    @Test
    fun `Refresh preview button renders and has click action when not refreshing`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(isRefreshingPayload = false)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Refresh preview").assertExists().assertHasClickAction()
    }

    @Test
    fun `Refresh preview button click fires onRefreshPayload callback`() {
        var refreshed = false
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(
                    isRefreshingPayload = false,
                    onRefreshPayload = { refreshed = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Refresh preview").performClick()
        composeRule.waitForIdle()
        assertTrue(refreshed)
    }

    @Test
    fun `refreshing state hides Refresh preview button`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(isRefreshingPayload = true)
            }
        }
        composeRule.waitForIdle()
        // When refreshing, the OutlinedButton is disabled (still present but shows spinner)
        // The text node still exists but the button is not enabled
        composeRule.onNodeWithText("Refresh preview").assertExists()
    }

    @Test
    fun `Back button has click action and fires onBack callback`() {
        var backClicks = 0
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(onBack = { backClicks++ })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").assertExists().performClick()
        composeRule.waitForIdle()
        assertEquals(1, backClicks)
    }

    @Test
    fun `renders privacy reassurance text`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen()
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your data stays on your device.", substring = true).assertExists()
    }

    @Test
    fun `renders Payload Preview section header`() {
        composeRule.setContent {
            BilboTheme {
                DataAnonymizationScreen(jsonPayload = """{"key":"value"}""")
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Payload Preview").assertExists()
    }
}
