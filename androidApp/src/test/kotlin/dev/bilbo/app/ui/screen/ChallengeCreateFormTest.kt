package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.social.ChallengeEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [CreateChallengeForm]. Covers:
 *  - Challenge title field and Type section visible on first render
 *  - Type picker radio buttons for EARN_NUTRITIVE_MINUTES and REDUCE_EMPTY_CALORIES
 *  - Mode picker chips (Competitive / Cooperative) reachable via LazyColumn scroll
 *  - Duration chips (3d / 7d / 14d / 30d) reachable via scroll
 *  - Create button does not fire when form is blank
 *  - Create button fires onCreate with correct values when form is filled
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChallengeCreateFormTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Scroll the LazyColumn to a node matching [text]. */
    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(text, substring = true))
        composeRule.waitForIdle()
    }


    @Test
    fun `challenge title field and Type section are in initial viewport`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Challenge title").assertExists()
        composeRule.onNodeWithText("Type").assertExists()
    }

    @Test
    fun `earn nutritive minutes and reduce empty calories type options render`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES.displayName(),
        ).assertExists()
        composeRule.onNodeWithText(
            ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES.displayName(),
        ).assertExists()
    }

    @Test
    fun `mode picker chips are reachable via LazyColumn scroll`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        scrollToText("Competitive")
        composeRule.onNodeWithText("Competitive").assertHasClickAction()
        composeRule.onNodeWithText("Cooperative").assertHasClickAction()
    }

    @Test
    fun `duration chips are reachable via LazyColumn scroll`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        scrollToText("3d")
        composeRule.onNodeWithText("3d").assertExists()
        composeRule.onNodeWithText("7d").assertExists()
        composeRule.onNodeWithText("14d").assertExists()
        composeRule.onNodeWithText("30d").assertExists()
    }

    @Test
    fun `Create Challenge button reachable via scroll`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        scrollToText("Create Challenge")
        composeRule.onNodeWithText("Create Challenge").assertExists()
    }

    @Test
    fun `Create Challenge button does not fire onCreate when form is empty`() {
        var created = false
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> created = true })
            }
        }
        composeRule.waitForIdle()
        scrollToText("Create Challenge")
        composeRule.onNodeWithText("Create Challenge").performClick()
        composeRule.waitForIdle()
        assertTrue(!created, "onCreate must not fire when title/target are blank")
    }

    @Test
    fun `switching mode to Cooperative via scroll and click`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        scrollToText("Cooperative")
        composeRule.onNodeWithText("Cooperative").performClick()
        composeRule.waitForIdle()
        // Both chips still render after toggling
        composeRule.onNodeWithText("Cooperative").assertExists()
        composeRule.onNodeWithText("Competitive").assertExists()
    }

    @Test
    fun `selecting 30d duration chip via scroll`() {
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(onCreate = { _, _, _, _, _, _ -> })
            }
        }
        composeRule.waitForIdle()
        scrollToText("30d")
        composeRule.onNodeWithText("30d").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("30d").assertExists()
    }

    @Test
    fun `Create Challenge fires onCreate with title type and team flag when form filled`() {
        var capturedType: ChallengeEngine.ChallengeType? = null
        var capturedTitle: String? = null
        var capturedIsTeam: Boolean? = null
        composeRule.setContent {
            BilboTheme {
                CreateChallengeForm(
                    onCreate = { title, type, isTeam, _, _, _ ->
                        capturedTitle = title
                        capturedType = type
                        capturedIsTeam = isTeam
                    },
                )
            }
        }
        composeRule.waitForIdle()
        // Enter title
        composeRule.onNodeWithText("Challenge title").performTextInput("Weekly Focus")
        composeRule.waitForIdle()
        // Enter target
        scrollToText("Target value")
        composeRule.onNodeWithText("Target value", substring = true).performTextInput("60")
        composeRule.waitForIdle()
        // Submit
        scrollToText("Create Challenge")
        composeRule.onNodeWithText("Create Challenge").performClick()
        composeRule.waitForIdle()
        assertNotNull(capturedType, "onCreate type should not be null")
        assertEquals("Weekly Focus", capturedTitle)
        assertEquals(false, capturedIsTeam) // default is Competitive → isTeam = false
    }
}
