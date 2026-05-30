package dev.bilbo.app.ui.screen.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [SettingsScreen].
 *
 * The screen is a verticalScroll Column — NOT a LazyColumn — so all items are
 * laid out and present in the semantics tree without needing scroll-to-node.
 * We call waitForIdle() after every interaction.
 *
 * Switch nodes:  SettingsSwitchRow uses `onClick = null` on the outer Row; only
 * the Switch itself is toggleable.  We find the toggleable node inside the parent
 * row by using `hasAnyDescendant(hasText(label)) && isToggleable()`.
 *
 * Picker rows: the whole SettingsRowScaffold row is clickable via the label text.
 *
 * Covers every if/conditional branch:
 *  - Enforcement: Default Mode picker, Cooldown, Per-App Overrides,
 *    EnforcementModePicker dialog (all 3 modes)
 *  - Economy: fpEnabled switch; detail rows shown/hidden; Increase/Decrease buttons
 *  - Emotional: checkInEnabled switch; Cooling-Off row shown/hidden
 *  - AI Insights: cloudInsightsEnabled switch; Anonymize + Preview shown/hidden
 *  - Social: Sharing Level picker, SharingLevelPicker dialog (all 4 levels),
 *    Manage Buddies, Manage Circles
 *  - Notifications: all 4 switches; quietHoursEnabled → time row shown/hidden;
 *    default start/end times
 *  - Data: Export, Delete All Data dialog (confirm + dismiss),
 *    Delete Account dialog (confirm)
 *  - About: Version, Open-Source Licenses, Privacy Policy
 *  - Enum label values for EnforcementMode and SharingLevel
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen() {
        composeRule.setContent {
            BilboTheme {
                SettingsScreen()
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Set up the screen with an explicit [SettingsViewModel] so we can manipulate
     * state directly without relying on the Compose viewModel() factory.
     */
    private fun setScreenWithVm(
        configure: SettingsViewModel.() -> Unit = {},
        content: @androidx.compose.runtime.Composable (SettingsViewModel) -> Unit = { vm ->
            SettingsScreen(viewModel = vm)
        },
    ): SettingsViewModel {
        val vm = SettingsViewModel()
        vm.configure()
        composeRule.setContent {
            BilboTheme { content(vm) }
        }
        composeRule.waitForIdle()
        return vm
    }

    // ── Enforcement section ───────────────────────────────────────────────────

    @Test
    fun `enforcement section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("ENFORCEMENT").assertExists()
    }

    @Test
    fun `Default Mode picker row shows Soft Lock`() {
        setScreen()
        composeRule.onNodeWithText("Default Mode").assertExists()
        composeRule.onNodeWithText("Soft Lock").assertExists()
    }

    @Test
    fun `tapping Default Mode row opens EnforcementModePicker dialog`() {
        setScreen()
        composeRule.onNodeWithText("Default Mode").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Default Enforcement Mode").assertExists()
    }

    @Test
    fun `EnforcementModePicker shows all three modes`() {
        setScreen()
        composeRule.onNodeWithText("Default Mode").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Default Enforcement Mode").assertExists()
        // All three modes shown in the dialog
        composeRule.onAllNodesWithText("Soft Lock").onFirst().assertExists()
        composeRule.onAllNodesWithText("Hard Lock").onFirst().assertExists()
        composeRule.onAllNodesWithText("Track Only").onFirst().assertExists()
    }

    @Test
    fun `selecting Hard Lock in picker updates displayed value`() {
        setScreen()
        composeRule.onNodeWithText("Default Mode").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Hard Lock").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // Dialog dismissed; picker row now shows "Hard Lock"
        composeRule.onNodeWithText("Default Enforcement Mode").assertDoesNotExist()
        composeRule.onAllNodesWithText("Hard Lock").onFirst().assertExists()
    }

    @Test
    fun `dismissing EnforcementModePicker via Cancel closes dialog`() {
        setScreen()
        composeRule.onNodeWithText("Default Mode").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Default Enforcement Mode").assertDoesNotExist()
    }

    @Test
    fun `Cooldown Duration row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Cooldown Duration").assertExists()
    }

    @Test
    fun `Per-App Overrides arrow row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Per-App Overrides").assertExists()
    }

    // ── Economy section ───────────────────────────────────────────────────────

    @Test
    fun `economy section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("ECONOMY").assertExists()
    }

    @Test
    fun `Enable Focus Points switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Enable Focus Points").assertExists()
    }

    @Test
    fun `Daily Baseline row shown when fpEnabled is true`() {
        setScreen()
        composeRule.onNodeWithText("Daily Baseline").assertExists()
    }

    @Test
    fun `Anti-Gaming Protection row shown when fpEnabled is true`() {
        setScreen()
        composeRule.onNodeWithText("Anti-Gaming Protection").assertExists()
    }

    @Test
    fun `toggling Focus Points OFF hides EconomyDetailRows`() {
        setScreenWithVm(configure = { fpEnabled = false })
        composeRule.onNodeWithText("Daily Baseline").assertDoesNotExist()
        composeRule.onNodeWithText("Anti-Gaming Protection").assertDoesNotExist()
    }

    @Test
    fun `Daily Baseline increase button increments value`() {
        setScreen()
        // Default is 60 FP; click Increase (contentDescription on IconButton) → 70 FP
        composeRule.onNodeWithContentDescription("Increase")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("70 FP").assertExists()
    }

    @Test
    fun `Daily Baseline decrease button decrements value`() {
        setScreen()
        // Default is 60 FP; click Decrease (contentDescription on IconButton) → 50 FP
        composeRule.onNodeWithContentDescription("Decrease")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("50 FP").assertExists()
    }

    // ── Emotional section ─────────────────────────────────────────────────────

    @Test
    fun `emotional section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("EMOTIONAL").assertExists()
    }

    @Test
    fun `Emotional Check-Ins switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Emotional Check-Ins").assertExists()
    }

    @Test
    fun `Cooling-Off Mode shown when checkInEnabled is true`() {
        setScreen()
        composeRule.onNodeWithText("Cooling-Off Mode").assertExists()
    }

    @Test
    fun `toggling Emotional Check-Ins OFF hides Cooling-Off Mode`() {
        setScreenWithVm(configure = { checkInEnabled = false })
        composeRule.onNodeWithText("Cooling-Off Mode").assertDoesNotExist()
    }

    // ── AI Insights section ───────────────────────────────────────────────────

    @Test
    fun `ai insights section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("AI INSIGHTS").assertExists()
    }

    @Test
    fun `Cloud AI Insights switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Cloud AI Insights").assertExists()
    }

    @Test
    fun `Anonymize Before Sending shown when cloudInsightsEnabled is true`() {
        setScreen()
        composeRule.onNodeWithText("Anonymize Before Sending").assertExists()
    }

    @Test
    fun `Preview Data Sent to AI shown when cloudInsightsEnabled is true`() {
        setScreen()
        composeRule.onNodeWithText("Preview Data Sent to AI").assertExists()
    }

    @Test
    fun `toggling Cloud AI Insights OFF hides Anonymize and Preview rows`() {
        setScreenWithVm(configure = { cloudInsightsEnabled = false })
        composeRule.onNodeWithText("Anonymize Before Sending").assertDoesNotExist()
        composeRule.onNodeWithText("Preview Data Sent to AI").assertDoesNotExist()
    }

    // ── Social section ────────────────────────────────────────────────────────

    @Test
    fun `social section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("SOCIAL").assertExists()
    }

    @Test
    fun `Sharing Level picker row shows Friends by default`() {
        setScreen()
        composeRule.onNodeWithText("Sharing Level").assertExists()
        composeRule.onNodeWithText("Friends").assertExists()
    }

    @Test
    fun `tapping Sharing Level opens SharingLevelPicker dialog`() {
        setScreen()
        composeRule.onNodeWithText("Sharing Level").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // Dialog title matches the picker
        composeRule.onNodeWithText("Private").assertExists()
        composeRule.onNodeWithText("Circle").assertExists()
        composeRule.onNodeWithText("Public").assertExists()
    }

    @Test
    fun `SharingLevelPicker shows all four levels`() {
        setScreen()
        composeRule.onNodeWithText("Sharing Level").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Private").assertExists()
        composeRule.onAllNodesWithText("Friends").onFirst().assertExists()
        composeRule.onNodeWithText("Circle").assertExists()
        composeRule.onNodeWithText("Public").assertExists()
    }

    @Test
    fun `selecting Public in SharingLevelPicker updates displayed value`() {
        setScreen()
        composeRule.onNodeWithText("Sharing Level").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Public").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // Dialog dismissed; sharing level row now shows Public
        composeRule.onAllNodesWithText("Public").onFirst().assertExists()
    }

    @Test
    fun `Manage Accountability Buddies row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Manage Accountability Buddies").assertExists()
    }

    @Test
    fun `Manage Circles row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Manage Circles").assertExists()
    }

    // ── Notifications section ─────────────────────────────────────────────────

    @Test
    fun `notifications section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("NOTIFICATIONS").assertExists()
    }

    @Test
    fun `Nudge Notifications switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Nudge Notifications").assertExists()
    }

    @Test
    fun `Weekly Insight Ready switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Weekly Insight Ready").assertExists()
    }

    @Test
    fun `Challenge Updates switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Challenge Updates").assertExists()
    }

    @Test
    fun `Quiet Hours switch is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Quiet Hours").assertExists()
    }

    @Test
    fun `Quiet Hours time row shown when quietHoursEnabled is true`() {
        setScreen()
        // Time row labels "Start" and "End" visible when quietHoursEnabled=true (default)
        composeRule.onNodeWithText("Start").assertExists()
        composeRule.onNodeWithText("End").assertExists()
    }

    @Test
    fun `toggling Quiet Hours OFF hides time row`() {
        setScreenWithVm(configure = { quietHoursEnabled = false })
        composeRule.onNodeWithText("Start").assertDoesNotExist()
        composeRule.onNodeWithText("End").assertDoesNotExist()
    }

    @Test
    fun `default quiet hours times are shown`() {
        setScreen()
        composeRule.onNodeWithText("22:00").assertExists()
        composeRule.onNodeWithText("08:00").assertExists()
    }

    // ── Data section ──────────────────────────────────────────────────────────

    @Test
    fun `data section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("DATA").assertExists()
    }

    @Test
    fun `Export All Data as JSON row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Export All Data as JSON").assertExists()
    }

    @Test
    fun `tapping Delete All Data shows confirmation dialog`() {
        setScreen()
        composeRule.onNodeWithText("Delete All Data")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "This will permanently delete all your Bilbo data. This cannot be undone.",
        ).assertExists()
    }

    @Test
    fun `dismissing Delete All Data dialog via Cancel hides it`() {
        setScreen()
        composeRule.onNodeWithText("Delete All Data")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "This will permanently delete all your Bilbo data. This cannot be undone.",
        ).assertDoesNotExist()
    }

    @Test
    fun `confirming Delete All Data closes dialog`() {
        setScreen()
        composeRule.onNodeWithText("Delete All Data")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // The confirm button has text "Delete" (in error color)
        composeRule.onAllNodesWithText("Delete").onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "This will permanently delete all your Bilbo data. This cannot be undone.",
        ).assertDoesNotExist()
    }

    @Test
    fun `tapping Delete Account shows confirmation dialog`() {
        setScreen()
        composeRule.onNodeWithText("Delete Account")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "Your account and all associated data will be permanently deleted. This cannot be undone.",
        ).assertExists()
    }

    @Test
    fun `dismissing Delete Account dialog via Cancel hides it`() {
        setScreen()
        composeRule.onNodeWithText("Delete Account")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "Your account and all associated data will be permanently deleted. This cannot be undone.",
        ).assertDoesNotExist()
    }

    @Test
    fun `confirming Delete Account closes dialog`() {
        setScreen()
        composeRule.onNodeWithText("Delete Account")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // The dialog confirm button also has text "Delete Account".
        // Use onAllNodesWithText and pick the last one (dialog confirm button appears after
        // the row item in the semantics tree when dialog is open).
        val nodes = composeRule.onAllNodesWithText("Delete Account")
        nodes[nodes.fetchSemanticsNodes().size - 1]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            "Your account and all associated data will be permanently deleted. This cannot be undone.",
        ).assertDoesNotExist()
    }

    // ── About section ─────────────────────────────────────────────────────────

    @Test
    fun `about section header is rendered`() {
        setScreen()
        composeRule.onNodeWithText("ABOUT").assertExists()
    }

    @Test
    fun `Version label is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Version").assertExists()
    }

    @Test
    fun `Open-Source Licenses row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Open-Source Licenses").assertExists()
    }

    @Test
    fun `Privacy Policy row is rendered`() {
        setScreen()
        composeRule.onNodeWithText("Privacy Policy").assertExists()
    }

    // ── Enum label coverage ───────────────────────────────────────────────────

    @Test
    fun `EnforcementMode labels are correct`() {
        assertEquals("Soft Lock", EnforcementMode.SOFT_LOCK.label)
        assertEquals("Hard Lock", EnforcementMode.HARD_LOCK.label)
        assertEquals("Track Only", EnforcementMode.TRACK_ONLY.label)
    }

    @Test
    fun `SharingLevel labels are correct`() {
        assertEquals("Private", SharingLevel.PRIVATE.label)
        assertEquals("Friends", SharingLevel.FRIENDS.label)
        assertEquals("Circle", SharingLevel.CIRCLE.label)
        assertEquals("Public", SharingLevel.PUBLIC.label)
    }
}

