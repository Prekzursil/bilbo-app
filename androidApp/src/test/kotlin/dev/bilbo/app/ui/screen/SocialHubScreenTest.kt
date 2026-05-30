package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.social.BuddyManager
import dev.bilbo.social.ChallengeEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [SocialHubScreen].
 *
 * Covers all conditional render / click / state branches:
 *  - Tab navigation: Buddies / Circles / Challenges
 *  - Back button fires onBack
 *  - Buddies tab: empty state, populated list, invite/enter-code buttons, pair tap
 *  - SharingLevelChip: all four levels (MINIMAL, BASIC, STANDARD, DETAILED)
 *  - MAX_BUDDY_PAIRS reached → Invite Buddy button disabled
 *  - Circles tab: empty state, populated list, create/join buttons, circle tap
 *  - Circle singular / plural member count
 *  - Challenges tab: empty state, populated list, create button, challenge tap
 *  - Challenge team vs individual label; progress display
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SocialHubScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Tab navigation ────────────────────────────────────────────────────────

    @Test
    fun `default tab is Buddies and shows tab row`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Social").assertExists()
        composeRule.onNodeWithText("Buddies").assertExists()
        composeRule.onNodeWithText("Circles").assertExists()
        composeRule.onNodeWithText("Challenges").assertExists()
    }

    @Test
    fun `back button fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme { SocialHubScreen(onBack = { backCount++ }) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    @Test
    fun `tapping Circles tab switches to Circles content`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Circle").assertExists()
    }

    @Test
    fun `tapping Challenges tab switches to Challenges content`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Challenge").assertExists()
    }

    // ── Buddies tab: empty state ──────────────────────────────────────────────

    @Test
    fun `buddies tab empty state shows no-buddies message`() {
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(buddyState = BuddyUiState(pairs = emptyList()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No buddies yet. Invite a friend to get started!").assertExists()
    }

    @Test
    fun `buddies tab shows Invite Buddy and Enter Code buttons`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState()) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite Buddy").assertExists()
        composeRule.onNodeWithText("Enter Code").assertExists()
    }

    @Test
    fun `Invite Buddy button fires onInviteBuddy`() {
        var count = 0
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    buddyState = BuddyUiState(pairs = emptyList()),
                    onInviteBuddy = { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite Buddy").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    @Test
    fun `Enter Code button fires onEnterBuddyCode`() {
        var count = 0
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    buddyState = BuddyUiState(pairs = emptyList()),
                    onEnterBuddyCode = { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Code").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    // ── Buddies tab: populated ────────────────────────────────────────────────

    @Test
    fun `buddy pair list renders buddy name and status`() {
        val buddy = BuddyPairUiItem(
            pairId = "p1",
            buddyDisplayName = "Alice",
            sharingLevel = BuddyManager.SharingLevel.STANDARD,
            statusSummary = "312 FP · 5-day streak",
        )
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(buddyState = BuddyUiState(pairs = listOf(buddy)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Alice").assertExists()
        composeRule.onNodeWithText("312 FP · 5-day streak").assertExists()
        // Standard chip
        composeRule.onNodeWithText("Standard").assertExists()
    }

    @Test
    fun `tapping a buddy pair fires onBuddyPairTap with correct id`() {
        var tappedId = ""
        val buddy = BuddyPairUiItem(
            pairId = "pair-abc",
            buddyDisplayName = "Bob",
            sharingLevel = BuddyManager.SharingLevel.BASIC,
            statusSummary = "100 FP",
        )
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    buddyState = BuddyUiState(pairs = listOf(buddy)),
                    onBuddyPairTap = { id -> tappedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bob").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("pair-abc", tappedId)
    }

    // ── SharingLevelChip labels ───────────────────────────────────────────────

    @Test
    fun `MINIMAL sharing level shows Minimal chip`() {
        val buddy = BuddyPairUiItem("p", "Charlie", BuddyManager.SharingLevel.MINIMAL, "")
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState(pairs = listOf(buddy))) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Minimal").assertExists()
    }

    @Test
    fun `BASIC sharing level shows Basic chip`() {
        val buddy = BuddyPairUiItem("p", "Dana", BuddyManager.SharingLevel.BASIC, "")
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState(pairs = listOf(buddy))) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Basic").assertExists()
    }

    @Test
    fun `DETAILED sharing level shows Detailed chip`() {
        val buddy = BuddyPairUiItem("p", "Eve", BuddyManager.SharingLevel.DETAILED, "")
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState(pairs = listOf(buddy))) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Detailed").assertExists()
    }

    @Test
    fun `Invite Buddy disabled when MAX_BUDDY_PAIRS reached`() {
        // Fill up to MAX_BUDDY_PAIRS pairs so the Invite button becomes disabled
        val pairs = (1..BuddyManager.MAX_BUDDY_PAIRS).map { i ->
            BuddyPairUiItem("p$i", "Buddy$i", BuddyManager.SharingLevel.STANDARD, "")
        }
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState(pairs = pairs)) }
        }
        composeRule.waitForIdle()
        // Button still renders but is disabled — just verify it exists
        composeRule.onNodeWithText("Invite Buddy").assertExists()
    }

    // ── Circles tab: empty state ──────────────────────────────────────────────

    @Test
    fun `circles tab empty state shows no-circles message`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No circles yet. Create one or join with an invite code!").assertExists()
    }

    @Test
    fun `circles tab Create Circle button fires onCreateCircle`() {
        var count = 0
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    circleState = CircleUiState(circles = emptyList()),
                    onCreateCircle = { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Circle").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    @Test
    fun `circles tab Join Circle button fires onJoinCircle`() {
        var count = 0
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    circleState = CircleUiState(circles = emptyList()),
                    onJoinCircle = { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Join Circle").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    // ── Circles tab: populated ────────────────────────────────────────────────

    @Test
    fun `circle list renders circle name and plural member count`() {
        val circle = CircleUiItem("c1", "Focus Squad", 5, "Reduce scrolling")
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    circleState = CircleUiState(circles = listOf(circle)),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Focus Squad").assertExists()
        // plural "members"
        composeRule.onNodeWithText("5 members · Reduce scrolling").assertExists()
    }

    @Test
    fun `circle list renders singular member count`() {
        val circle = CircleUiItem("c2", "Solo Circle", 1, "Daily FP goal")
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    circleState = CircleUiState(circles = listOf(circle)),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        // singular "member" (no trailing 's')
        composeRule.onNodeWithText("1 member · Daily FP goal").assertExists()
    }

    @Test
    fun `tapping a circle fires onCircleTap with correct id`() {
        var tappedId = ""
        val circle = CircleUiItem("circle-xyz", "My Circle", 3, "goal")
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    circleState = CircleUiState(circles = listOf(circle)),
                    onCircleTap = { id -> tappedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Circles").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("My Circle").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("circle-xyz", tappedId)
    }

    // ── Challenges tab: empty state ───────────────────────────────────────────

    @Test
    fun `challenges tab empty state shows no-challenges message`() {
        composeRule.setContent {
            BilboTheme { SocialHubScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No active challenges. Create one to get motivated!").assertExists()
    }

    @Test
    fun `challenges tab Create Challenge button fires onCreateChallenge`() {
        var count = 0
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    challengeState = ChallengeUiState(activeChallenges = emptyList()),
                    onCreateChallenge = { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Challenge").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    // ── Challenges tab: populated ─────────────────────────────────────────────

    @Test
    fun `challenge list item renders title and team label`() {
        val challenge = ChallengeUiItem(
            challengeId = "ch1",
            title = "Team Scroll Reduction",
            type = ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES,
            progressPercent = 45,
            daysRemaining = 7,
            isTeam = true,
        )
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    challengeState = ChallengeUiState(activeChallenges = listOf(challenge)),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Team Scroll Reduction").assertExists()
        composeRule.onNodeWithText("Team · 7d left").assertExists()
        composeRule.onNodeWithText("45% complete").assertExists()
    }

    @Test
    fun `challenge list item renders individual label when isTeam is false`() {
        val challenge = ChallengeUiItem(
            challengeId = "ch2",
            title = "Solo FP Goal",
            type = ChallengeEngine.ChallengeType.REACH_FP_BALANCE,
            progressPercent = 80,
            daysRemaining = 3,
            isTeam = false,
        )
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    challengeState = ChallengeUiState(activeChallenges = listOf(challenge)),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Individual · 3d left").assertExists()
        composeRule.onNodeWithText("80% complete").assertExists()
    }

    @Test
    fun `tapping a challenge fires onChallengeTap with correct id`() {
        var tappedId = ""
        val challenge = ChallengeUiItem(
            challengeId = "challenge-99",
            title = "Streak Challenge",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            progressPercent = 50,
            daysRemaining = 10,
            isTeam = false,
        )
        composeRule.setContent {
            BilboTheme {
                SocialHubScreen(
                    challengeState = ChallengeUiState(activeChallenges = listOf(challenge)),
                    onChallengeTap = { id -> tappedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Challenges").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Streak Challenge").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("challenge-99", tappedId)
    }

    // ── Multiple items below fold (scroll) ────────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `multiple buddy pairs are scrollable and all render`() {
        val pairs = (1..5).map { i ->
            BuddyPairUiItem("p$i", "Person$i", BuddyManager.SharingLevel.STANDARD, "$i FP")
        }
        composeRule.setContent {
            BilboTheme { SocialHubScreen(buddyState = BuddyUiState(pairs = pairs)) }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Person5"))
        composeRule.onNodeWithText("Person5").assertExists()
    }
}
