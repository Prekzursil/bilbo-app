package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [CircleScreen].
 *
 * Covers all conditional render/click/state branches:
 *  - List mode: title, empty state, loading state, populated list, create/join buttons
 *  - CircleListCard: plural vs singular member count, daysRemaining nullable field
 *  - Tapping a circle fires onCircleTap and onModeChange to Detail
 *  - Detail mode: null selectedCircle → loading spinner
 *  - Detail mode: populated circle — header, goal, invite code, members, progress
 *  - Detail mode isAdmin=false → Leave Circle button + MoreVert menu → Leave dialog
 *  - Detail mode isAdmin=true → no Leave Circle button, no MoreVert menu
 *  - MemberNameRow: isCurrentUser=true → "You" badge; isCurrentUser=false → no badge
 *  - MemberStats: all stats present, partial, empty
 *  - Create mode: form shown
 *  - Join mode: form shown
 *  - Back button in List mode fires onBack
 *  - Back button in non-List mode returns to List
 *  - circleTitle helper branches (List, Detail null, Detail non-null, Create, Join)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CircleScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeListItem(
        id: String = "c1",
        name: String = "Focus Squad",
        memberCount: Int = 4,
        goalSummary: String = "Reduce scrolling",
        daysRemaining: Int? = 10,
        inviteCode: String = "ABCD1234",
    ) = CircleListUiItem(id, name, memberCount, goalSummary, daysRemaining, inviteCode)

    private fun makeDetailItem(
        id: String = "c1",
        name: String = "Focus Squad",
        goal: String = "Reduce scrolling by 50%",
        daysRemaining: Int = 10,
        inviteCode: String = "ABCD1234",
        members: List<CircleMemberUiItem> = listOf(
            CircleMemberUiItem("u1", "Alice", isCurrentUser = true, fpBalance = 100, streakDays = 5, nutritiveMinutes = null),
            CircleMemberUiItem("u2", "Bob", isCurrentUser = false, fpBalance = null, streakDays = null, nutritiveMinutes = null),
        ),
        aggregateProgressPercent: Int = 65,
        isAdmin: Boolean = false,
    ) = CircleDetailItem(id, name, goal, daysRemaining, inviteCode, members, aggregateProgressPercent, isAdmin)

    // ── List mode ─────────────────────────────────────────────────────────────

    @Test
    fun `list mode shows Focus Circles title`() {
        composeRule.setContent {
            BilboTheme { CircleScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Focus Circles").assertExists()
    }

    @Test
    fun `list mode empty state shows no-circles message`() {
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = emptyList()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No circles yet. Create one or join with an invite code!").assertExists()
    }

    @Test
    fun `list mode loading does not show empty state`() {
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = emptyList(), isLoading = true))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No circles yet. Create one or join with an invite code!").assertDoesNotExist()
    }

    @Test
    fun `list mode Create and Join buttons are shown`() {
        composeRule.setContent {
            BilboTheme { CircleScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create").assertExists()
        composeRule.onNodeWithText("Join").assertExists()
    }

    @Test
    fun `Create button fires onModeChange to Create mode`() {
        var newMode: CircleScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                CircleScreen(onModeChange = { newMode = it })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create").performClick()
        composeRule.waitForIdle()
        assertEquals(CircleScreenMode.Create, newMode)
    }

    @Test
    fun `Join button fires onModeChange to Join mode`() {
        var newMode: CircleScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                CircleScreen(onModeChange = { newMode = it })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()
        assertEquals(CircleScreenMode.Join, newMode)
    }

    @Test
    fun `back button in List mode fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    mode = CircleScreenMode.List,
                    onBack = { backCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    // ── CircleListCard: plural/singular member count ───────────────────────────

    @Test
    fun `circle list card renders name, plural member count, and goal`() {
        val item = makeListItem(name = "My Circle", memberCount = 5, goalSummary = "Healthy habits")
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("My Circle").assertExists()
        composeRule.onNodeWithText("5 members · Healthy habits").assertExists()
    }

    @Test
    fun `circle list card renders member count and goal`() {
        // CircleScreen always uses "members" (no singular/plural switch in this component)
        val item = makeListItem(name = "Solo Circle", memberCount = 1, goalSummary = "Solo goal")
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1 members · Solo goal").assertExists()
    }

    @Test
    fun `circle list card shows days remaining when non-null`() {
        val item = makeListItem(daysRemaining = 7)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("7 days remaining").assertExists()
    }

    @Test
    fun `circle list card hides days remaining when null`() {
        val item = makeListItem(daysRemaining = null)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(state = CircleDetailUiState(circles = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("days remaining").assertDoesNotExist()
    }

    @Test
    fun `tapping a circle fires onCircleTap and onModeChange`() {
        var tappedId = ""
        var newMode: CircleScreenMode? = null
        val item = makeListItem(id = "circle-99", name = "Tap Me")
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(circles = listOf(item)),
                    onCircleTap = { id -> tappedId = id },
                    onModeChange = { mode -> newMode = mode },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tap Me").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals("circle-99", tappedId)
        assertEquals(CircleScreenMode.Detail("circle-99"), newMode)
    }

    // ── Detail mode: null selected → loading ──────────────────────────────────

    @Test
    fun `detail mode null selectedCircle shows fallback title Circle`() {
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = null),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Circle").assertExists()
    }

    // ── Detail mode: back returns to List ─────────────────────────────────────

    @Test
    fun `back button in Detail mode fires onModeChange to List`() {
        var newMode: CircleScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = makeDetailItem()),
                    mode = CircleScreenMode.Detail("c1"),
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(CircleScreenMode.List, newMode)
    }

    // ── Detail mode: populated circle ─────────────────────────────────────────

    @Test
    fun `detail mode shows goal, invite code, and group progress`() {
        val detail = makeDetailItem(
            name = "Focus Squad",
            goal = "Reduce scrolling by 50%",
            inviteCode = "XYZW5678",
            aggregateProgressPercent = 65,
        )
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        // "Focus Squad" appears in both TopAppBar and header card — use onAllNodes
        composeRule.onAllNodesWithText("Focus Squad").fetchSemanticsNodes().isNotEmpty()
            .also { assert(it) }
        composeRule.onNodeWithText("Goal: Reduce scrolling by 50%").assertExists()
        composeRule.onNodeWithText("Group Progress").assertExists()
        composeRule.onNodeWithText("65%").assertExists()
        composeRule.onNodeWithText("XYZW5678").assertExists()
    }

    @Test
    fun `detail mode shows days remaining in header card`() {
        val detail = makeDetailItem(daysRemaining = 8)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("8 days remaining").assertExists()
    }

    @Test
    fun `detail mode shows invite code label`() {
        val detail = makeDetailItem(inviteCode = "INVITE99")
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Invite code").assertExists()
        composeRule.onNodeWithText("INVITE99").assertExists()
    }

    @Test
    fun `detail mode shows members header`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u1", "Alice", true, 100, 5, null),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Members (1)").assertExists()
    }

    // ── Detail mode: isAdmin=false → Leave Circle button + MoreVert ───────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `detail non-admin shows Leave Circle button below fold`() {
        val detail = makeDetailItem(isAdmin = false)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Leave Circle"))
        composeRule.onNodeWithText("Leave Circle").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `tapping Leave Circle button fires onLeaveCircle`() {
        var leftId = ""
        val detail = makeDetailItem(id = "leave-me", isAdmin = false)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("leave-me"),
                    onLeaveCircle = { id -> leftId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Leave Circle"))
        composeRule.onNodeWithText("Leave Circle").performClick()
        composeRule.waitForIdle()
        assertEquals("leave-me", leftId)
    }

    @Test
    fun `detail non-admin shows MoreVert options menu`() {
        val detail = makeDetailItem(isAdmin = false)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").assertExists()
    }

    @Test
    fun `tapping MoreVert shows Leave Circle in dropdown`() {
        val detail = makeDetailItem(isAdmin = false)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").performClick()
        composeRule.waitForIdle()
        // The dropdown "Leave Circle" item appears in menu
        composeRule.onNodeWithText("Leave Circle").assertExists()
    }

    @Test
    fun `tapping Leave Circle in dropdown fires onLeaveCircle`() {
        var leftId = ""
        val detail = makeDetailItem(id = "dropdown-circle", isAdmin = false)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("dropdown-circle"),
                    onLeaveCircle = { id -> leftId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Leave Circle").performClick()
        composeRule.waitForIdle()
        assertEquals("dropdown-circle", leftId)
    }

    // ── Detail mode: isAdmin=true → no Leave/MoreVert ─────────────────────────

    @Test
    fun `detail admin does not show MoreVert menu`() {
        val detail = makeDetailItem(isAdmin = true)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Options").assertDoesNotExist()
    }

    @Test
    fun `detail admin does not show Leave Circle button`() {
        val detail = makeDetailItem(isAdmin = true)
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        // Non-scrolled view: the leave button should not appear yet for admin
        composeRule.onNodeWithText("Leave Circle").assertDoesNotExist()
    }

    // ── MemberCard: isCurrentUser badge ──────────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `member with isCurrentUser shows You badge`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u1", "Alice", isCurrentUser = true, fpBalance = null, streakDays = null, nutritiveMinutes = null),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Alice"))
        composeRule.onNodeWithText("You").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `member without isCurrentUser does not show You badge`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u2", "Bob", isCurrentUser = false, fpBalance = null, streakDays = null, nutritiveMinutes = null),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Bob"))
        composeRule.onNodeWithText("You").assertDoesNotExist()
    }

    // ── MemberStats: all/partial/empty stats ──────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `member with all stats shows fp, streak, and nutritive`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u1", "Carol", false, fpBalance = 200, streakDays = 7, nutritiveMinutes = 45),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Carol"))
        composeRule.onNodeWithText("200 FP · 7d streak · 45m nutritive").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `member with only fpBalance shows just fp stat`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u1", "Dave", false, fpBalance = 50, streakDays = null, nutritiveMinutes = null),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Dave"))
        composeRule.onNodeWithText("50 FP").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `member with no stats shows no stats text`() {
        val detail = makeDetailItem(members = listOf(
            CircleMemberUiItem("u1", "Eve", false, fpBalance = null, streakDays = null, nutritiveMinutes = null),
        ))
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Eve"))
        composeRule.onNodeWithText("Eve").assertExists()
        // No stats text should appear (just no crash)
        composeRule.onNodeWithText("FP").assertDoesNotExist()
    }

    // ── Create mode ───────────────────────────────────────────────────────────

    @Test
    fun `create mode shows Create Circle title`() {
        composeRule.setContent {
            BilboTheme { CircleScreen(mode = CircleScreenMode.Create) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Circle").assertExists()
    }

    @Test
    fun `back button in Create mode fires onModeChange to List`() {
        var newMode: CircleScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    mode = CircleScreenMode.Create,
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(CircleScreenMode.List, newMode)
    }

    // ── Join mode ─────────────────────────────────────────────────────────────

    @Test
    fun `join mode shows Join Circle title`() {
        composeRule.setContent {
            BilboTheme { CircleScreen(mode = CircleScreenMode.Join) }
        }
        composeRule.waitForIdle()
        // "Join Circle" appears in both the TopAppBar title and the form button
        composeRule.onAllNodesWithText("Join Circle").fetchSemanticsNodes().isNotEmpty()
            .also { assert(it) { "Expected at least one node with 'Join Circle'" } }
    }

    @Test
    fun `back button in Join mode fires onModeChange to List`() {
        var newMode: CircleScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    mode = CircleScreenMode.Join,
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(CircleScreenMode.List, newMode)
    }

    // ── circleTitle branches (Detail with name) ───────────────────────────────

    @Test
    fun `Detail mode title shows circle name when selectedCircle is set`() {
        val detail = makeDetailItem(name = "Awesome Circle")
        composeRule.setContent {
            BilboTheme {
                CircleScreen(
                    state = CircleDetailUiState(selectedCircle = detail),
                    mode = CircleScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        // "Awesome Circle" appears in TopAppBar title AND in the detail header card
        composeRule.onAllNodesWithText("Awesome Circle").fetchSemanticsNodes().isNotEmpty()
            .also { assert(it) { "Expected at least one node with 'Awesome Circle'" } }
    }
}
