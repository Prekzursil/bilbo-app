package dev.bilbo.app.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.Emotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [PostSessionMoodScreen].
 *
 * The screen wraps its card in `AnimatedVisibility(visible)` where `visible`
 * starts `false` and is set to `true` by `LaunchedEffect(Unit) { visible = true }`.
 * A second `LaunchedEffect` runs a 10-second countdown then auto-dismisses.
 *
 * Clock control strategy:
 *   1. Freeze clock before setContent so launched coroutines don't auto-advance.
 *   2. advanceTimeBy(1) fires `LaunchedEffect { visible = true }` (no delay).
 *   3. advanceTimeBy(500) completes the enter animation (340ms slide) while staying
 *      well below the first countdown tick (1000ms).
 *
 * Click callbacks are exercised via `performSemanticsAction(SemanticsActions.OnClick)`
 * which directly invokes the semantics click action, bypassing any layout-level hit
 * testing that can block gestures in animated containers.
 *
 * Covers: header, countdown text, Skip present, Skip callback, Happy callback,
 * Calm callback, all 7 emotion labels in tree.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PostSessionMoodScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContentAndShow(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { BilboTheme { content() } }
        // Fire LaunchedEffect { visible = true } (zero delay, runs on 1ms advance)
        composeRule.mainClock.advanceTimeBy(1L)
        // Advance past the 340ms enter animation; stay under 1000ms countdown tick
        composeRule.mainClock.advanceTimeBy(500L)
    }

    @Test
    fun `header question text is visible`() {
        setContentAndShow { PostSessionMoodScreen(onMoodSelected = {}, onSkip = {}) }
        composeRule.onNodeWithText("How do you feel now?").assertExists()
    }

    @Test
    fun `countdown text is rendered`() {
        setContentAndShow { PostSessionMoodScreen(onMoodSelected = {}, onSkip = {}) }
        composeRule.onNodeWithText("Auto-dismisses in 10 s").assertExists()
    }

    @Test
    fun `Skip text is in the tree`() {
        setContentAndShow { PostSessionMoodScreen(onMoodSelected = {}, onSkip = {}) }
        composeRule.onNodeWithText("Skip").assertExists()
    }

    @Test
    fun `tapping Skip fires onSkip callback via semantics action`() {
        var skipCount = 0
        setContentAndShow {
            PostSessionMoodScreen(onMoodSelected = {}, onSkip = { skipCount++ })
        }
        composeRule.onNodeWithText("Skip")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, skipCount)
    }

    @Test
    fun `Happy chip fires HAPPY via semantics action`() {
        var captured: Emotion? = null
        setContentAndShow {
            PostSessionMoodScreen(onMoodSelected = { captured = it }, onSkip = {})
        }
        composeRule.onNodeWithText("Happy")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(Emotion.HAPPY, captured)
    }

    @Test
    fun `Calm chip fires CALM via semantics action`() {
        var captured: Emotion? = null
        setContentAndShow {
            PostSessionMoodScreen(onMoodSelected = { captured = it }, onSkip = {})
        }
        composeRule.onNodeWithText("Calm")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(Emotion.CALM, captured)
    }

    @Test
    fun `all seven emotion labels are present in the tree`() {
        setContentAndShow { PostSessionMoodScreen(onMoodSelected = {}, onSkip = {}) }
        listOf("Happy", "Calm", "Bored", "Stressed", "Anxious", "Sad", "Lonely").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun `Skip node has OnClick semantics action`() {
        setContentAndShow { PostSessionMoodScreen(onMoodSelected = {}, onSkip = {}) }
        composeRule.onNodeWithText("Skip")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
    }
}
