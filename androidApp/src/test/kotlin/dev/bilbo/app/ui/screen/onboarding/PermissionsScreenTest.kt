package dev.bilbo.app.ui.screen.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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

/**
 * Robolectric Compose UI tests for [PermissionsScreen].
 *
 * The screen reads real Android system state (UsageAccess, CanDrawOverlays,
 * POST_NOTIFICATIONS) via [buildPermissionItems].  Under Robolectric with
 * sdk=33 these are all denied by default, so:
 *   - "Grant" buttons are shown (not checkmarks)
 *   - "Continue" button is DISABLED (allGranted == false)
 *   - "Skip for now" TextButton IS shown
 *
 * Covers:
 *  - header title and subtitle rendered
 *  - back button fires onBack
 *  - the 3 permission card titles present (Usage Access, Display Over Other Apps, Notifications)
 *  - "Grant" buttons present (permissions denied in test environment)
 *  - Continue is present (disabled state branch)
 *  - Skip for now visible when not all granted
 *  - tapping Skip for now fires onContinue
 *  - Continue button click fires onContinue (Compose dispatches even when disabled=false semantics;
 *    we verify the button exists, testing the allGranted=false disabled branch)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `header title and subtitle rendered`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Permissions").assertExists()
        composeRule.onNodeWithText("Bilbo needs these to protect your focus.").assertExists()
    }

    @Test
    fun `back button fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = { backCount++ })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `usage access permission card title is shown`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Usage Access").assertExists()
        composeRule.onNodeWithText("So Bilbo can see which apps are running").assertExists()
    }

    @Test
    fun `overlay permission card title is shown`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Display Over Other Apps").assertExists()
        composeRule.onNodeWithText("So Bilbo can show you the Intent Gatekeeper").assertExists()
    }

    @Test
    fun `notifications permission card title is shown`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Notifications").assertExists()
        composeRule.onNodeWithText("So Bilbo can remind you when time is up").assertExists()
    }

    @Test
    fun `Grant buttons rendered when permissions denied (Robolectric default)`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        // At least one Grant button must be present — Robolectric has no granted permissions
        composeRule.onAllNodesWithText("Grant").onFirst().assertExists()
    }

    @Test
    fun `Skip for now button shown when not all permissions granted`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        // allGranted == false branch shows "Skip for now"
        composeRule.onNodeWithText("Skip for now").assertExists()
    }

    @Test
    fun `tapping Skip for now fires onContinue`() {
        var continueCount = 0
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = { continueCount++ }, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Skip for now").performClick()
        composeRule.waitForIdle()
        assertEquals(1, continueCount)
    }

    @Test
    fun `Continue button disabled branch exists (not all permissions granted)`() {
        composeRule.setContent {
            BilboTheme {
                PermissionsScreen(onContinue = {}, onBack = {})
            }
        }
        composeRule.waitForIdle()
        // The Continue button renders even when disabled; just verify it's in the tree
        composeRule.onNodeWithText("Continue").assertExists()
    }
}
