package dev.bilbo.app.ui.screen.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [FirstIntentScreen].
 *
 * The screen owns a `currentStep` mutableState advancing through five [DemoStep]
 * enum entries. AnimatedContent wraps both the step-card area and the coaching
 * text; during transitions both the exiting and entering nodes are present in the
 * semantics tree.
 *
 * Strategy: autoAdvance = true so state changes propagate normally; after each
 * click we call waitForIdle() which drains the frame queue.  Then we locate
 * nodes by text that must be unique to a particular step.
 *
 * Covers:
 *  - initial title + INTRO coaching text + "Try it" button
 *  - INTRO card shows "Tap an app icon…"
 *  - Back button only on step 0 (INTRO)
 *  - Back button fires onBack callback
 *  - step GATEKEEPER_SHOWN card content
 *  - step INTENT_ENTERED card content ("Intent logged")
 *  - step TIMER_SHOWN card content ("remaining of 10 min")
 *  - step COMPLETE card (+10 Focus Points)
 *  - "Complete Setup" button fires onCompleteSetup callback
 *  - all five steps navigated without crash
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FirstIntentScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(onComplete: () -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            BilboTheme {
                FirstIntentScreen(onCompleteSetup = onComplete, onBack = onBack)
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Click the first node with [label] and wait for idle.
     * Uses onAllNodesWithText + onFirst to tolerate the case where
     * AnimatedContent briefly keeps both the outgoing and incoming node.
     */
    private fun clickLabel(label: String) {
        composeRule.onAllNodesWithText(label).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `title Let's try it out is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Let's try it out!").assertExists()
    }

    @Test
    fun `initial coaching text is shown on INTRO step`() {
        setScreen()
        composeRule.onNodeWithText(
            "When you open a tracked app, Bilbo pauses and asks: why are you here?",
        ).assertExists()
    }

    @Test
    fun `initial button label is Try it on INTRO step`() {
        setScreen()
        composeRule.onNodeWithText("Try it").assertExists()
    }

    @Test
    fun `INTRO card shows Tap an app icon content`() {
        setScreen()
        composeRule.onNodeWithText("Tap an app icon…").assertExists()
    }

    @Test
    fun `Back button is shown on the first step INTRO`() {
        setScreen()
        // showBack = (stepIndex == 0) → TextButton "← Back" is in the tree
        composeRule.onNodeWithText("← Back").assertExists()
    }

    @Test
    fun `Back button fires onBack callback`() {
        var backCount = 0
        setScreen(onBack = { backCount++ })
        composeRule.onNodeWithText("← Back").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `tapping Try it advances to GATEKEEPER_SHOWN and shows I see it button`() {
        setScreen()
        clickLabel("Try it")
        // The GATEKEEPER_SHOWN step shows "I see it" as the button label
        composeRule.onAllNodesWithText("I see it").onFirst().assertExists()
    }

    @Test
    fun `GATEKEEPER_SHOWN card shows How long 10 min text`() {
        setScreen()
        clickLabel("Try it")
        // GatekeeperCard has a Text "How long? 10 min"
        composeRule.onAllNodesWithText("How long? 10 min").onFirst().assertExists()
    }

    @Test
    fun `INTENT_ENTERED step shows Intent logged card`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        composeRule.onAllNodesWithText("Intent logged").onFirst().assertExists()
    }

    @Test
    fun `INTENT_ENTERED coaching text is shown`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        composeRule.onAllNodesWithText(
            "You stated your intent. Bilbo notes this and tracks if you followed through.",
        ).onFirst().assertExists()
    }

    @Test
    fun `TIMER_SHOWN step shows time remaining card`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        composeRule.onAllNodesWithText("remaining of 10 min").onFirst().assertExists()
    }

    @Test
    fun `TIMER_SHOWN coaching text is shown`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        composeRule.onAllNodesWithText(
            "A gentle timer reminds you of your stated session length.",
        ).onFirst().assertExists()
    }

    @Test
    fun `COMPLETE step shows Plus 10 Focus Points card`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        clickLabel("Makes sense")
        composeRule.onAllNodesWithText("+10 Focus Points").onFirst().assertExists()
    }

    @Test
    fun `COMPLETE step shows Complete Setup button`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        clickLabel("Makes sense")
        composeRule.onAllNodesWithText("Complete Setup").onFirst().assertExists()
    }

    @Test
    fun `COMPLETE step coaching text is shown`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        clickLabel("Makes sense")
        composeRule.onAllNodesWithText(
            "You're all set! Bilbo will now help you stay intentional every day.",
        ).onFirst().assertExists()
    }

    @Test
    fun `COMPLETE step earned for completing intent text is shown`() {
        setScreen()
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        clickLabel("Makes sense")
        composeRule.onAllNodesWithText("Earned for completing your intent!").onFirst().assertExists()
    }

    @Test
    fun `Complete Setup button fires onCompleteSetup callback`() {
        var completeCount = 0
        setScreen(onComplete = { completeCount++ })
        clickLabel("Try it")
        clickLabel("I see it")
        clickLabel("Got it")
        clickLabel("Makes sense")
        clickLabel("Complete Setup")
        assertEquals(1, completeCount)
    }

    @Test
    fun `all five steps can be navigated without crash`() {
        var completeCount = 0
        setScreen(onComplete = { completeCount++ })
        val labels = listOf("Try it", "I see it", "Got it", "Makes sense", "Complete Setup")
        for (label in labels) {
            composeRule.onAllNodesWithText(label).onFirst()
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.waitForIdle()
        }
        assertTrue(completeCount == 1, "onCompleteSetup should be called once")
    }
}
