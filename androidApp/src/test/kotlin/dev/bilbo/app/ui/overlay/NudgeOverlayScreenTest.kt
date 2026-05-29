package dev.bilbo.app.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * Robolectric Compose UI tests for [NudgeOverlayScreen]. Covers every branch:
 *  - canExtend == true (renders extend button, click invokes callback)
 *  - canExtend == false (renders insufficient-balance text instead)
 *  - actualMinutes == 1 vs != 1 (pluralisation of "minute(s)")
 *  - Got-it click invokes onGotIt
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NudgeOverlayScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `renders title, plural minutes, and extend button when balance is enough`() {
        composeRule.setContent {
            BilboTheme {
                NudgeOverlayScreen(
                    appName = "Instagram",
                    declaredMinutes = 10,
                    actualMinutes = 15,
                    fpBalance = 20,
                    onGotIt = {},
                    onExtend5Min = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("You've been here for 15 minutes.").assertExists()
        composeRule.onNodeWithText("5 more minutes  (−5 FP)").assertExists()
        composeRule.onNodeWithText("Got it").assertExists()
    }

    @Test
    fun `singular minute pluralisation when actualMinutes is 1`() {
        composeRule.setContent {
            BilboTheme {
                NudgeOverlayScreen(
                    appName = "Reddit",
                    declaredMinutes = 5,
                    actualMinutes = 1,
                    fpBalance = 10,
                    onGotIt = {},
                    onExtend5Min = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("You've been here for 1 minute.").assertExists()
    }

    @Test
    fun `cannot-extend branch shows insufficient balance text`() {
        composeRule.setContent {
            BilboTheme {
                NudgeOverlayScreen(
                    appName = "TikTok",
                    declaredMinutes = 5,
                    actualMinutes = 8,
                    fpBalance = 2,
                    onGotIt = {},
                    onExtend5Min = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Balance: 2 FP · Not enough to extend").assertExists()
    }

    @Test
    fun `Got it click invokes callback`() {
        var clicks = 0
        composeRule.setContent {
            BilboTheme {
                NudgeOverlayScreen(
                    appName = "Twitter",
                    declaredMinutes = 5,
                    actualMinutes = 5,
                    fpBalance = 10,
                    onGotIt = { clicks++ },
                    onExtend5Min = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Got it").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `Extend click invokes callback only on the enabled branch`() {
        var extendClicks = 0
        composeRule.setContent {
            BilboTheme {
                NudgeOverlayScreen(
                    appName = "YouTube",
                    declaredMinutes = 10,
                    actualMinutes = 10,
                    fpBalance = 5,
                    onGotIt = {},
                    onExtend5Min = { extendClicks++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("5 more minutes  (−5 FP)").performClick()
        assertEquals(1, extendClicks)
    }
}
