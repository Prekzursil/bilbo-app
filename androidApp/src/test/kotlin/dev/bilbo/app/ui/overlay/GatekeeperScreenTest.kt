package dev.bilbo.app.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.tracking.AppInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [GatekeeperScreen].
 *
 * The screen uses `LaunchedEffect(Unit) { visible = true }` + `animateFloatAsState`
 * to slide the card in.  We freeze the main clock, fire the LaunchedEffect, then
 * advance past the 380 ms slide before asserting.
 *
 * Duration chips are ordinary [FilterChip] composables inside a Row.
 * After tapping a chip we advance the clock slightly and call waitForIdle so
 * recomposition (updating `GatekeeperFormState.selectedDuration`) settles and the
 * StartButton recomposes with the new label.
 *
 * Covers:
 *  - app label and initials abbreviation shown in header
 *  - section labels rendered
 *  - placeholder text when intention is empty
 *  - character counter at 0/100 initially
 *  - all six duration chips rendered (5m 10m 15m 20m 30m 1h)
 *  - Start button default label "Start 15 min"
 *  - tapping 5m, 30m, 1h chips updates Start button label
 *  - tapping Start fires onStart with empty trimmed intention + default duration
 *  - tapping Start fires onStart after selecting a different duration
 *  - "Not now" fires onDismiss
 *  - durationLabel branch for 60 (1h) and < 60 (Nm)
 *  - long app label: first two chars uppercased as initials
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GatekeeperScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val testAppInfo = AppInfo(
        packageName = "com.example.instagram",
        appLabel = "Instagram",
        category = null,
    )

    private fun setScreen(
        appInfo: AppInfo = testAppInfo,
        onStart: (String, Int) -> Unit = { _, _ -> },
        onDismiss: () -> Unit = {},
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            BilboTheme {
                GatekeeperScreen(appInfo = appInfo, onStart = onStart, onDismiss = onDismiss)
            }
        }
        // Fire LaunchedEffect { visible = true } (runs on first frame)
        composeRule.mainClock.advanceTimeBy(1L)
        // Advance past the 380 ms slide animation
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()
    }

    /** Click the first node with [label] and let recomposition settle. */
    private fun clickAndSettle(label: String) {
        composeRule.onAllNodesWithText(label).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeBy(100L)
        composeRule.waitForIdle()
    }

    // ── Header / app info ─────────────────────────────────────────────────────

    @Test
    fun `app label is shown in header`() {
        setScreen()
        composeRule.onNodeWithText("Instagram").assertExists()
    }

    @Test
    fun `app initials abbreviation is shown in icon circle`() {
        setScreen()
        // AppHeader renders appLabel.take(2).uppercase() → "IN"
        composeRule.onNodeWithText("IN").assertExists()
    }

    @Test
    fun `section labels are rendered`() {
        setScreen()
        composeRule.onNodeWithText("What's your intention?").assertExists()
        composeRule.onNodeWithText("How long?").assertExists()
    }

    // ── Intention field ───────────────────────────────────────────────────────

    @Test
    fun `placeholder text visible when intention is empty`() {
        setScreen()
        composeRule.onNodeWithText("e.g. check my messages (optional)").assertExists()
    }

    @Test
    fun `character counter starts at 0 of 100`() {
        setScreen()
        composeRule.onNodeWithText("0/100").assertExists()
    }

    @Test
    fun `typing in intention field updates character counter`() {
        setScreen()
        // autoAdvance is false; re-enable for text input recomposition
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("e.g. check my messages (optional)")
            .performTextInput("Hello")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("5/100").assertExists()
    }

    // ── Duration chips ────────────────────────────────────────────────────────

    @Test
    fun `all six duration chip labels are rendered`() {
        setScreen()
        listOf("5m", "10m", "15m", "20m", "30m", "1h").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun `Start button default label is Start 15 min`() {
        setScreen()
        // Default selected duration is DURATION_15 = 15
        composeRule.onNodeWithText("Start 15 min").assertExists()
    }

    @Test
    fun `tapping 5m chip changes Start button label to Start 5 min`() {
        setScreen()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("5m").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 5 min").assertExists()
    }

    @Test
    fun `tapping 30m chip changes Start button label to Start 30 min`() {
        setScreen()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("30m").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 30 min").assertExists()
    }

    @Test
    fun `tapping 1h chip changes Start button label to Start 60 min`() {
        setScreen()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("1h").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 60 min").assertExists()
    }

    // ── Start button ──────────────────────────────────────────────────────────

    @Test
    fun `tapping Start fires onStart with empty intention and default 15 min duration`() {
        var capturedIntention: String? = null
        var capturedDuration: Int? = null
        setScreen(onStart = { intention, duration ->
            capturedIntention = intention
            capturedDuration = duration
        })
        composeRule.onNodeWithText("Start 15 min").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("", capturedIntention)
        assertEquals(15, capturedDuration)
    }

    @Test
    fun `tapping Start fires onStart with typed intention`() {
        var capturedIntention: String? = null
        setScreen(onStart = { intention, _ -> capturedIntention = intention })
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("e.g. check my messages (optional)")
            .performTextInput("Check my messages")
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Start 15 min").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("Check my messages", capturedIntention)
    }

    @Test
    fun `tapping Start after selecting 10m fires onStart with duration 10`() {
        var capturedDuration: Int? = null
        setScreen(onStart = { _, duration -> capturedDuration = duration })
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("10m").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 10 min").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(10, capturedDuration)
    }

    // ── Not now / dismiss ─────────────────────────────────────────────────────

    @Test
    fun `tapping Not now fires onDismiss`() {
        var dismissCount = 0
        setScreen(onDismiss = { dismissCount++ })
        composeRule.onNodeWithText("Not now").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, dismissCount)
    }

    // ── Edge cases / durationLabel branches ───────────────────────────────────

    @Test
    fun `durationLabel returns 1h for 60 minutes`() {
        // Verify via rendered chip text — durationLabel maps 60 → "1h"
        setScreen()
        composeRule.onNodeWithText("1h").assertExists()
    }

    @Test
    fun `durationLabel returns Nm format for values under 60`() {
        setScreen()
        listOf("5m", "10m", "15m", "20m", "30m").forEach { label ->
            assertTrue(label.endsWith("m"), "expected '$label' to end with 'm'")
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun `long app label first two chars shown as initials`() {
        val youtubeApp = AppInfo(
            packageName = "com.example.youtube",
            appLabel = "YouTube",
            category = null,
        )
        setScreen(appInfo = youtubeApp)
        // "YouTube".take(2).uppercase() → "YO"
        composeRule.onNodeWithText("YO").assertExists()
        composeRule.onNodeWithText("YouTube").assertExists()
    }

    @Test
    fun `multiple duration chips can be selected in sequence`() {
        setScreen()
        composeRule.mainClock.autoAdvance = true

        composeRule.onNodeWithText("5m").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 5 min").assertExists()

        composeRule.onNodeWithText("20m").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 20 min").assertExists()

        composeRule.onNodeWithText("1h").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Start 60 min").assertExists()
    }

    @Test
    fun `intention is trimmed before passing to onStart`() {
        var capturedIntention: String? = null
        setScreen(onStart = { intention, _ -> capturedIntention = intention })
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("e.g. check my messages (optional)")
            .performTextInput("  trimmed  ")
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Start 15 min").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("trimmed", capturedIntention)
    }
}
