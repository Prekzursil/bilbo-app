package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.SuggestionCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [InterestsOnboardingScreen].
 *
 * Covers:
 *  - screen renders without crash
 *  - heading text is displayed
 *  - "Select at least 2 interests" shown when < 2 selected
 *  - "Continue (N selected)" shown when >= 2 selected
 *  - toggling a chip adds/removes from selection
 *  - onContinue callback invoked on button click (when enabled)
 *  - initialSelections pre-populates chips
 *  - all 10 interest chip labels visible
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InterestsOnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Render ────────────────────────────────────────────────────────────────

    @Test
    fun `screen renders without crash`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `heading text is displayed`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("What do you enjoy offline?").assertIsDisplayed()
    }

    @Test
    fun `subtitle text is displayed`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "We'll suggest activities that match your interests when you feel like reaching for your phone.",
        ).assertExists()
    }

    // ── Button state ──────────────────────────────────────────────────────────

    @Test
    fun `button shows select-at-least-2 when no selection`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Select at least 2 interests").assertIsDisplayed()
    }

    @Test
    fun `button shows select-at-least-2 when one interest selected`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = setOf(SuggestionCategory.EXERCISE),
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Select at least 2 interests").assertIsDisplayed()
    }

    @Test
    fun `button shows Continue(N selected) when 2 interests selected`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = setOf(SuggestionCategory.EXERCISE, SuggestionCategory.COOKING),
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue (2 selected)").assertIsDisplayed()
    }

    @Test
    fun `button shows Continue(N selected) when 3 interests selected`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = setOf(
                        SuggestionCategory.EXERCISE,
                        SuggestionCategory.COOKING,
                        SuggestionCategory.MUSIC,
                    ),
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue (3 selected)").assertIsDisplayed()
    }

    // ── onContinue callback ───────────────────────────────────────────────────

    @Test
    fun `onContinue is called when Continue button is clicked`() {
        var callbackFired = false
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = setOf(SuggestionCategory.EXERCISE, SuggestionCategory.MUSIC),
                    onContinue = { callbackFired = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue (2 selected)").performClick()
        composeRule.waitForIdle()
        assertTrue(callbackFired)
    }

    // ── Chip toggle ───────────────────────────────────────────────────────────

    @Test
    fun `clicking a chip adds it to selection`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
        // Start: 0 selected — click "💪 Exercise"
        composeRule.onNodeWithText("💪 Exercise").performClick()
        composeRule.waitForIdle()
        // 1 selected — still shows "Select at least 2 interests"
        composeRule.onNodeWithText("Select at least 2 interests").assertIsDisplayed()
        // Click "🎵 Music"
        composeRule.onNodeWithText("🎵 Music").performClick()
        composeRule.waitForIdle()
        // Now 2 selected — button becomes "Continue"
        composeRule.onNodeWithText("Continue (2 selected)").assertIsDisplayed()
    }

    @Test
    fun `clicking a selected chip removes it from selection`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = setOf(SuggestionCategory.EXERCISE, SuggestionCategory.MUSIC),
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        // 2 selected → deselect one
        composeRule.onNodeWithText("💪 Exercise").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Select at least 2 interests").assertIsDisplayed()
    }

    // ── All chip labels visible ───────────────────────────────────────────────

    @Test
    fun `all interest chip labels are displayed`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(onContinue = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("📚 Reading").assertExists()
        composeRule.onNodeWithText("💪 Exercise").assertExists()
        composeRule.onNodeWithText("🍳 Cooking").assertExists()
        composeRule.onNodeWithText("🎨 Art").assertExists()
        composeRule.onNodeWithText("🎵 Music").assertExists()
        composeRule.onNodeWithText("🌿 Nature").assertExists()
        composeRule.onNodeWithText("👥 Social").assertExists()
        composeRule.onNodeWithText("🧘 Mindfulness").assertExists()
        composeRule.onNodeWithText("🎲 Physical Games").assertExists()
        composeRule.onNodeWithText("📖 Learning").assertExists()
    }

    // ── initialSelections ─────────────────────────────────────────────────────

    @Test
    fun `empty initialSelections starts with no selection`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(initialSelections = emptySet(), onContinue = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Select at least 2 interests").assertIsDisplayed()
    }

    @Test
    fun `all interests pre-selected renders Continue with 10 selected`() {
        composeRule.setContent {
            BilboTheme {
                InterestsOnboardingScreen(
                    initialSelections = SuggestionCategory.entries.toSet(),
                    onContinue = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Continue (10 selected)").assertIsDisplayed()
    }
}
