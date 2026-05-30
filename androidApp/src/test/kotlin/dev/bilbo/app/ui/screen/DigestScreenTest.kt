package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [DigestScreen].
 *
 * Items below the fold in the LazyColumn are scrolled into view via
 * performScrollToNode on the scrollable container.
 *
 * Covers:
 *  - default state renders title and "Got it" button
 *  - close button (X) fires onDismiss
 *  - "Got it" button fires onDismiss
 *  - loading branch (no DigestList hero banner)
 *  - weekLabel displayed
 *  - topAnalogSuggestion shown only when non-blank
 *  - topAnalogSuggestion NOT shown when blank
 *  - topCircleAchievement shown only when non-blank
 *  - topCircleAchievement NOT shown when blank
 *  - anonymousTips shown when non-empty
 *  - anonymousTips NOT shown when empty
 *  - formatCompact M, K, raw number branches
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DigestScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Scrolls the LazyColumn to bring the node with the given text into view. */
    @OptIn(ExperimentalTestApi::class)
    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(text))
    }

    @Test
    fun `default state renders Community Digest title and Got it button`() {
        composeRule.setContent {
            BilboTheme { DigestScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Community Digest").assertExists()
        composeRule.onNodeWithText("Got it").assertExists()
    }

    @Test
    fun `close button fires onDismiss`() {
        var dismissCount = 0
        composeRule.setContent {
            BilboTheme { DigestScreen(onDismiss = { dismissCount++ }) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.waitForIdle()
        assertEquals(1, dismissCount)
    }

    @Test
    fun `Got it button fires onDismiss`() {
        var dismissCount = 0
        composeRule.setContent {
            BilboTheme { DigestScreen(onDismiss = { dismissCount++ }) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Got it").performClick()
        composeRule.waitForIdle()
        assertEquals(1, dismissCount)
    }

    @Test
    fun `loading branch does not show hero banner`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(data = CommunityDigestData(isLoading = true))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This Week in the Bilbo Community").assertDoesNotExist()
    }

    @Test
    fun `non-loading branch shows hero banner and week label`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(isLoading = false, weekLabel = "May 26–Jun 1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This Week in the Bilbo Community").assertExists()
        composeRule.onNodeWithText("May 26–Jun 1").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `topAnalogSuggestion shown when non-blank`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        topAnalogSuggestion = "Go for a walk in nature",
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        scrollToText("Top Analog Pick")
        composeRule.onNodeWithText("Top Analog Pick").assertExists()
        composeRule.onNodeWithText("Go for a walk in nature").assertExists()
    }

    @Test
    fun `topAnalogSuggestion not shown when blank`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(isLoading = false, topAnalogSuggestion = ""),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Top Analog Pick").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `topCircleAchievement shown when non-blank`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        topCircleAchievement = "Team went screen-free for 72 hours!",
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        scrollToText("Circle Achievement")
        composeRule.onNodeWithText("Circle Achievement").assertExists()
        composeRule.onNodeWithText("Team went screen-free for 72 hours!").assertExists()
    }

    @Test
    fun `topCircleAchievement not shown when blank`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(isLoading = false, topCircleAchievement = ""),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Circle Achievement").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `anonymous tips shown when non-empty`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        anonymousTips = listOf("Take a cold shower", "Call a friend"),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        scrollToText("From the Community")
        composeRule.onNodeWithText("From the Community").assertExists()
        scrollToText("\"Take a cold shower\"")
        composeRule.onNodeWithText("\"Take a cold shower\"").assertExists()
    }

    @Test
    fun `anonymous tips section not shown when empty`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(isLoading = false, anonymousTips = emptyList()),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("From the Community").assertDoesNotExist()
    }

    @Test
    fun `active users formatted as M for values over 1 million`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        totalActiveUsers = 2_500_000,
                        collectiveHoursSaved = 500,
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2M").assertExists()
        composeRule.onNodeWithText("500h").assertExists()
    }

    @Test
    fun `active users formatted as K for values over 1 thousand`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        totalActiveUsers = 3_200,
                        collectiveHoursSaved = 42,
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3K").assertExists()
        composeRule.onNodeWithText("42h").assertExists()
    }

    @Test
    fun `active users formatted as raw number when under 1000`() {
        composeRule.setContent {
            BilboTheme {
                DigestScreen(
                    data = CommunityDigestData(
                        isLoading = false,
                        totalActiveUsers = 987,
                        collectiveHoursSaved = 5,
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("987").assertExists()
        composeRule.onNodeWithText("5h").assertExists()
    }
}
