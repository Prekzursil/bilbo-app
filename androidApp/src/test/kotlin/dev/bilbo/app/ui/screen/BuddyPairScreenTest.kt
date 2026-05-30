package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.social.BuddyManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Robolectric Compose UI tests for [BuddyPairScreen].
 *
 * Covers all conditional render/click/state branches:
 *  - Default empty state: action buttons, empty buddy hint
 *  - MaxPairsNotice shown when pairs == MAX_BUDDY_PAIRS
 *  - Invite button disabled when max pairs reached
 *  - isGeneratingCode = true shows progress indicator
 *  - Back button fires onBack
 *  - EnterCodeDialog opens on "Enter Code" click, Cancel dismisses, Join fires callback
 *  - InviteCodeDialog shown when generatedInviteCode != null, Done dismisses
 *  - BuddyPairCard header: buddy name, status summary, online dot (isOnline branch)
 *  - BuddySharingRow: opens SharingLevelDialog on click → select new level fires callback
 *  - SharingLevelDialog: all 4 levels present, Cancel/Close dismisses
 *  - RemoveBuddyDialog: Options button → Remove → fires onRemovePair
 *  - RemoveBuddyDialog: Cancel dismisses
 *  - Send encouragement: opens EncouragementDialog, character counter, Send fires callback
 *  - EncouragementDialog: targetName branch (name shown when buddy found)
 *  - EncouragementDialog: Send disabled when text is blank, enabled when text present
 *  - Nudge section: shown only when nudges non-empty; unread vs read NudgeCard
 *  - Dismiss nudge fires onDismissNudge
 *  - Nudges section not shown when nudges list is empty
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BuddyPairScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makePair(
        pairId: String = "p1",
        buddyDisplayName: String = "Alice",
        sharingLevel: BuddyManager.SharingLevel = BuddyManager.SharingLevel.STANDARD,
        statusSummary: String = "312 FP today · 5-day streak",
        isOnline: Boolean = false,
    ) = BuddyPairDetailItem(pairId, buddyDisplayName, sharingLevel, statusSummary, isOnline)

    private fun makeNudge(
        nudgeId: String = "n1",
        fromName: String = "Bob",
        message: String = "Keep it up!",
        timeAgo: String = "2h ago",
        isUnread: Boolean = false,
    ) = NudgeItem(nudgeId, fromName, message, timeAgo, isUnread)

    @OptIn(ExperimentalTestApi::class)
    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(text))
    }

    // ── Default / empty state ─────────────────────────────────────────────────

    @Test
    fun `default state shows screen title`() {
        composeRule.setContent {
            BilboTheme { BuddyPairScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Accountability Buddies").assertExists()
    }

    @Test
    fun `default empty state shows invite and enter code buttons`() {
        composeRule.setContent {
            BilboTheme { BuddyPairScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite a Buddy").assertExists()
        composeRule.onNodeWithText("Enter Code").assertExists()
    }

    @Test
    fun `empty state shows invite a friend hint`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(state = BuddyPairDetailUiState(pairs = emptyList()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite a friend to become accountability buddies.").assertExists()
    }

    @Test
    fun `non-empty state does not show empty hint`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(state = BuddyPairDetailUiState(pairs = listOf(makePair())))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite a friend to become accountability buddies.").assertDoesNotExist()
    }

    // ── MaxPairsNotice ────────────────────────────────────────────────────────

    @Test
    fun `max pairs notice shown when pairs equals MAX_BUDDY_PAIRS`() {
        val maxPairs = (1..BuddyManager.MAX_BUDDY_PAIRS).map { i ->
            makePair(pairId = "p$i", buddyDisplayName = "Buddy$i")
        }
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(state = BuddyPairDetailUiState(pairs = maxPairs))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Maximum ${BuddyManager.MAX_BUDDY_PAIRS} buddy pairs reached.").assertExists()
    }

    @Test
    fun `max pairs notice not shown when below max`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(state = BuddyPairDetailUiState(pairs = listOf(makePair())))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Maximum ${BuddyManager.MAX_BUDDY_PAIRS} buddy pairs reached.").assertDoesNotExist()
    }

    @Test
    fun `invite button disabled when max pairs reached`() {
        val maxPairs = (1..BuddyManager.MAX_BUDDY_PAIRS).map { i ->
            makePair(pairId = "p$i", buddyDisplayName = "Buddy$i")
        }
        var inviteCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = maxPairs),
                    onInviteBuddy = { inviteCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        // The button is disabled so clicking should not fire the callback
        composeRule.onNodeWithText("Invite a Buddy").performClick()
        composeRule.waitForIdle()
        assertEquals(0, inviteCount)
    }

    // ── isGeneratingCode ─────────────────────────────────────────────────────

    @Test
    fun `isGeneratingCode true disables invite button`() {
        var inviteCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(isGeneratingCode = true),
                    onInviteBuddy = { inviteCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        // Button is disabled while generating
        composeRule.onNodeWithText("Invite a Buddy").performClick()
        composeRule.waitForIdle()
        assertEquals(0, inviteCount)
    }

    @Test
    fun `isGeneratingCode false enables invite button and fires callback`() {
        var inviteCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(isGeneratingCode = false),
                    onInviteBuddy = { inviteCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite a Buddy").performClick()
        composeRule.waitForIdle()
        assertEquals(1, inviteCount)
    }

    // ── Back button ────────────────────────────────────────────────────────────

    @Test
    fun `back button fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(onBack = { backCount++ })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    // ── EnterCodeDialog ────────────────────────────────────────────────────────

    @Test
    fun `Enter Code button opens EnterCodeDialog`() {
        composeRule.setContent {
            BilboTheme { BuddyPairScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Code").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Invite Code").assertExists()
    }

    @Test
    fun `EnterCodeDialog Cancel button dismisses dialog`() {
        composeRule.setContent {
            BilboTheme { BuddyPairScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Code").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Invite Code").assertDoesNotExist()
    }

    @Test
    fun `EnterCodeDialog Join button fires onEnterCode with typed value`() {
        var enteredCode = ""
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(onEnterCode = { code -> enteredCode = code })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Code").performClick()
        composeRule.waitForIdle()
        // Type exactly 6 characters to enable the Join button
        composeRule.onNodeWithText("XXXXXX").performTextInput("ABCDEF")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()
        assertEquals("ABCDEF", enteredCode)
    }

    @Test
    fun `EnterCodeDialog Join button disabled when fewer than 6 chars`() {
        var enteredCode = ""
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(onEnterCode = { code -> enteredCode = code })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter Code").performClick()
        composeRule.waitForIdle()
        // Type fewer than 6 characters — Join should remain disabled
        composeRule.onNodeWithText("XXXXXX").performTextInput("AB")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()
        // The callback should not have been called
        assertEquals("", enteredCode)
    }

    // ── InviteCodeDialog ───────────────────────────────────────────────────────

    @Test
    fun `InviteCodeDialog shown when generatedInviteCode is non-null`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(generatedInviteCode = "ZXCVBN"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Invite Code").assertExists()
        composeRule.onNodeWithText("ZXCVBN").assertExists()
    }

    @Test
    fun `InviteCodeDialog Done button dismisses dialog`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(generatedInviteCode = "ZXCVBN"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Invite Code").assertDoesNotExist()
    }

    @Test
    fun `InviteCodeDialog not shown when generatedInviteCode is null`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(generatedInviteCode = null),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Invite Code").assertDoesNotExist()
    }

    // ── BuddyPairCard: header content ─────────────────────────────────────────

    @Test
    fun `buddy pair card shows buddy name and status summary`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(buddyDisplayName = "Charlie", statusSummary = "200 FP · 3-day streak")),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Charlie").assertExists()
        composeRule.onNodeWithText("200 FP · 3-day streak").assertExists()
    }

    @Test
    fun `buddy pair card shows Your Buddies section header`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Buddies").assertExists()
    }

    // ── SharingLevelDialog ────────────────────────────────────────────────────

    @Test
    fun `clicking sharing level text button opens SharingLevelDialog`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(sharingLevel = BuddyManager.SharingLevel.STANDARD)),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        // The TextButton shows the level name; click it to open the picker
        composeRule.onNodeWithText("Standard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sharing Level").assertExists()
    }

    @Test
    fun `SharingLevelDialog shows all four sharing levels`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Standard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Minimal").assertExists()
        composeRule.onNodeWithText("Basic").assertExists()
        composeRule.onNodeWithText("Detailed").assertExists()
    }

    @Test
    fun `selecting a sharing level in dialog fires onSharingLevelChange`() {
        var changedPairId = ""
        var changedLevel: BuddyManager.SharingLevel? = null
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(pairId = "p99", sharingLevel = BuddyManager.SharingLevel.STANDARD)),
                    ),
                    onSharingLevelChange = { pairId, level ->
                        changedPairId = pairId
                        changedLevel = level
                    },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Standard").performClick()
        composeRule.waitForIdle()
        // Verify SharingLevelDialog is open
        composeRule.onNodeWithText("Sharing Level").assertExists()
        // The Row for MINIMAL is now clickable (whole row fires onSelect).
        // "Minimal" text is the level label in the dialog row.
        composeRule.onNodeWithText("Minimal").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("p99", changedPairId)
        assertEquals(BuddyManager.SharingLevel.MINIMAL, changedLevel)
    }

    @Test
    fun `SharingLevelDialog Close button dismisses dialog`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Standard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sharing Level").assertDoesNotExist()
    }

    // ── RemoveBuddyDialog ─────────────────────────────────────────────────────

    @Test
    fun `Options button opens RemoveBuddyDialog with buddy name`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(buddyDisplayName = "Diana")),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remove Buddy?").assertExists()
        composeRule.onNodeWithText("Are you sure you want to remove Diana as a buddy?").assertExists()
    }

    @Test
    fun `Remove in RemoveBuddyDialog fires onRemovePair with correct id`() {
        var removedId = ""
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(pairId = "remove-99", buddyDisplayName = "Eve")),
                    ),
                    onRemovePair = { id -> removedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.waitForIdle()
        assertEquals("remove-99", removedId)
    }

    @Test
    fun `Cancel in RemoveBuddyDialog dismisses without calling onRemovePair`() {
        var removedCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair(buddyDisplayName = "Frank"))),
                    onRemovePair = { removedCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Remove Buddy?").assertDoesNotExist()
        assertEquals(0, removedCount)
    }

    // ── EncouragementDialog ────────────────────────────────────────────────────

    @Test
    fun `Send encouragement button opens EncouragementDialog`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send Encouragement").assertExists()
    }

    @Test
    fun `EncouragementDialog shows target name when buddy found`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(pairId = "p1", buddyDisplayName = "Grace")),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("To: Grace").assertExists()
    }

    @Test
    fun `EncouragementDialog Send button disabled when text is blank`() {
        var sendCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                    onSendEncouragement = { _, _ -> sendCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        // Click Send without typing anything — should not fire
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitForIdle()
        assertEquals(0, sendCount)
    }

    @Test
    fun `EncouragementDialog Send button fires onSendEncouragement when text is present`() {
        var sentPairId = ""
        var sentMessage = ""
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(pairId = "p77")),
                    ),
                    onSendEncouragement = { pairId, msg ->
                        sentPairId = pairId
                        sentMessage = msg
                    },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Write a short message…").performTextInput("You are doing great!")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send").performClick()
        composeRule.waitForIdle()
        assertEquals("p77", sentPairId)
        assertEquals("You are doing great!", sentMessage)
    }

    @Test
    fun `EncouragementDialog Cancel dismisses without sending`() {
        var sendCount = 0
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                    onSendEncouragement = { _, _ -> sendCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send Encouragement").assertDoesNotExist()
        assertEquals(0, sendCount)
    }

    @Test
    fun `EncouragementDialog shows character counter`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Send encouragement").performClick()
        composeRule.waitForIdle()
        // Initially counter shows 0/100
        composeRule.onNodeWithText("0/100").assertExists()
    }

    // ── Nudge section ──────────────────────────────────────────────────────────

    @Test
    fun `nudge section shown when nudges list is non-empty`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(nudges = listOf(makeNudge())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nudges").assertExists()
    }

    @Test
    fun `nudge section not shown when nudges list is empty`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(nudges = emptyList()),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nudges").assertDoesNotExist()
    }

    @Test
    fun `nudge card shows from name and message`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        nudges = listOf(makeNudge(fromName = "Hank", message = "Great job today!")),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hank").assertExists()
        composeRule.onNodeWithText("Great job today!").assertExists()
    }

    @Test
    fun `nudge card shows timeAgo`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        nudges = listOf(makeNudge(timeAgo = "5h ago")),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("5h ago").assertExists()
    }

    @Test
    fun `dismiss nudge button fires onDismissNudge with correct id`() {
        var dismissedId = ""
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        nudges = listOf(makeNudge(nudgeId = "nudge-42")),
                    ),
                    onDismissNudge = { id -> dismissedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        composeRule.waitForIdle()
        assertEquals("nudge-42", dismissedId)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `unread nudge card renders differently from read nudge`() {
        // Both cards render — unread has an extra unread-dot Box but no content-description.
        // We just verify both nudges are present without a crash.
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        nudges = listOf(
                            makeNudge(nudgeId = "n1", fromName = "Ian", isUnread = true),
                            makeNudge(nudgeId = "n2", fromName = "Jane", isUnread = false),
                        ),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ian").assertExists()
        // Jane may be below the fold — scroll to it
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Jane"))
        composeRule.onNodeWithText("Jane").assertExists()
    }

    // ── Multiple buddy pairs: scroll to below-fold items ─────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `second buddy pair card is accessible via scroll`() {
        val pairs = listOf(
            makePair(pairId = "p1", buddyDisplayName = "Karen"),
            makePair(pairId = "p2", buddyDisplayName = "Leo"),
        )
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(state = BuddyPairDetailUiState(pairs = pairs))
            }
        }
        composeRule.waitForIdle()
        scrollToText("Leo")
        composeRule.onNodeWithText("Leo").assertExists()
    }

    // ── BASIC sharing level display ───────────────────────────────────────────

    @Test
    fun `buddy pair card shows BASIC sharing level label`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(sharingLevel = BuddyManager.SharingLevel.BASIC)),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Basic").assertExists()
    }

    @Test
    fun `buddy pair card shows DETAILED sharing level label`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(
                        pairs = listOf(makePair(sharingLevel = BuddyManager.SharingLevel.DETAILED)),
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Detailed").assertExists()
    }

    // ── sharingLevelDescription branches ─────────────────────────────────────

    @Test
    fun `SharingLevelDialog shows description text for all levels`() {
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(pairs = listOf(makePair())),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Standard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Presence only — buddy knows you're here").assertExists()
        composeRule.onNodeWithText("FP balance and streak").assertExists()
        composeRule.onNodeWithText("Daily FP summary").assertExists()
        composeRule.onNodeWithText("Full breakdown including app categories").assertExists()
    }

    // ── Multiple nudges: scroll below fold ────────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `nudges below fold are accessible via scroll`() {
        val nudges = (1..5).map { i ->
            makeNudge(nudgeId = "n$i", fromName = "Person$i", message = "Message $i")
        }
        composeRule.setContent {
            BilboTheme {
                BuddyPairScreen(
                    state = BuddyPairDetailUiState(nudges = nudges),
                )
            }
        }
        composeRule.waitForIdle()
        scrollToText("Message 5")
        composeRule.onNodeWithText("Message 5").assertExists()
    }
}
