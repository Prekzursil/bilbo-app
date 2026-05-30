package dev.bilbo.app.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.Emotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [AIInterventionCard].
 *
 * Covers:
 *  - card renders without crash
 *  - app name appears in "Continue to …" button
 *  - breathing button is displayed
 *  - onBreathe callback is invoked when breathing button tapped
 *  - onContinue callback is invoked when continue button tapped
 *  - postMood = null does not crash (no "Afterward" text)
 *  - postMood = non-null shows "Afterward…" text
 *  - all Emotion variants in PatternObservationText render their label
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AIInterventionCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Basic render ─────────────────────────────────────────────────────────

    @Test
    fun `AIInterventionCard renders without crash`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.BORED,
                    appName = "Instagram",
                    avgDurationMins = 20,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        // No crash = pass
    }

    @Test
    fun `breathing button is displayed`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.STRESSED,
                    appName = "TikTok",
                    avgDurationMins = 15,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Yes, let me breathe 🌬️").assertIsDisplayed()
    }

    @Test
    fun `continue button shows app name`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.ANXIOUS,
                    appName = "YouTube",
                    avgDurationMins = 30,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue to YouTube").assertIsDisplayed()
    }

    // ── Callbacks ────────────────────────────────────────────────────────────

    @Test
    fun `onBreathe is called when breathing button is tapped`() {
        var breatheCalled = false
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.SAD,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = { breatheCalled = true },
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Yes, let me breathe 🌬️").performClick()
        composeRule.waitForIdle()
        assertTrue(breatheCalled)
    }

    @Test
    fun `onContinue is called when continue button is tapped`() {
        var continueCalled = false
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.LONELY,
                    appName = "Twitter",
                    avgDurationMins = 25,
                    postMood = null,
                    onBreathe = {},
                    onContinue = { continueCalled = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue to Twitter").performClick()
        composeRule.waitForIdle()
        assertTrue(continueCalled)
    }

    // ── postMood branches ────────────────────────────────────────────────────

    @Test
    fun `null postMood does not crash and no Afterward text shown`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.CALM,
                    appName = "App",
                    avgDurationMins = 5,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Afterward you usually feel", substring = true).assertDoesNotExist()
    }

    @Test
    fun `non-null postMood shows Afterward text`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.BORED,
                    appName = "App",
                    avgDurationMins = 20,
                    postMood = Emotion.SAD,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Afterward you usually feel", substring = true).assertExists()
    }

    // ── Emotion display labels ────────────────────────────────────────────────

    @Test
    fun `HAPPY emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.HAPPY,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("happy 😊", substring = true).assertExists()
    }

    @Test
    fun `CALM emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.CALM,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("calm 😌", substring = true).assertExists()
    }

    @Test
    fun `BORED emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.BORED,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("bored 😑", substring = true).assertExists()
    }

    @Test
    fun `STRESSED emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.STRESSED,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("stressed 😫", substring = true).assertExists()
    }

    @Test
    fun `ANXIOUS emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.ANXIOUS,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("anxious 😰", substring = true).assertExists()
    }

    @Test
    fun `SAD emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.SAD,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("sad 😢", substring = true).assertExists()
    }

    @Test
    fun `LONELY emotion label renders`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.LONELY,
                    appName = "App",
                    avgDurationMins = 10,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("lonely 😔", substring = true).assertExists()
    }

    // ── Breathing prompt text ────────────────────────────────────────────────

    @Test
    fun `breathing prompt text is displayed`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.STRESSED,
                    appName = "App",
                    avgDurationMins = 15,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Would you like to try 2 minutes", substring = true).assertExists()
    }

    // ── avgDurationMins appears in pattern text ──────────────────────────────

    @Test
    fun `avg duration minutes text appears in pattern observation`() {
        composeRule.setContent {
            BilboTheme {
                AIInterventionCard(
                    emotion = Emotion.BORED,
                    appName = "App",
                    avgDurationMins = 42,
                    postMood = null,
                    onBreathe = {},
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("42 min", substring = true).assertExists()
    }
}
