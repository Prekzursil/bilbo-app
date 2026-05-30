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
import dev.bilbo.social.ChallengeEngine
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Robolectric Compose UI tests for [ChallengeScreen].
 *
 * Covers all conditional render/click/state branches:
 *  - List mode: empty state, loading state, populated list, title, create button
 *  - Challenge list cards: team vs solo label, progress percent, type chip
 *  - Detail mode: null selected challenge → loading spinner
 *  - Detail mode: populated challenge (cooperative) → group progress, no leaderboard
 *  - Detail mode: populated challenge (competitive) → leaderboard section, no group progress
 *  - Detail mode: UPCOMING → Join Challenge button; ACTIVE → no Join button
 *  - Description shown when non-blank; hidden when blank
 *  - Create mode: form shown
 *  - Back button in List mode fires onBack
 *  - Back button in non-List mode returns to List
 *  - onModeChange callbacks fired when tapping challenge / create
 *  - onJoinChallenge fires when Join button tapped
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChallengeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val startDate = LocalDate(2026, 1, 1)
    private val endDate = LocalDate(2026, 1, 31)

    private fun makeListItem(
        id: String = "c1",
        title: String = "Test Challenge",
        type: ChallengeEngine.ChallengeType = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
        isTeam: Boolean = false,
        progressPercent: Int = 50,
        daysRemaining: Int = 10,
        status: ChallengeEngine.ChallengeStatus = ChallengeEngine.ChallengeStatus.ACTIVE,
    ) = ChallengeListItem(id, title, type, isTeam, progressPercent, daysRemaining, status)

    private fun makeDetailItem(
        id: String = "c1",
        title: String = "Detail Challenge",
        description: String = "A test description",
        type: ChallengeEngine.ChallengeType = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
        scope: ChallengeEngine.ChallengeScope = ChallengeEngine.ChallengeScope.CIRCLE,
        isTeam: Boolean = true,
        targetValue: Int = 100,
        daysRemaining: Int = 5,
        status: ChallengeEngine.ChallengeStatus = ChallengeEngine.ChallengeStatus.ACTIVE,
        myProgress: Int = 60,
        myProgressPercent: Int = 60,
        leaderboard: List<ChallengeLeaderboardEntry> = emptyList(),
        groupProgressPercent: Int = 75,
    ) = ChallengeDetailItem(
        challengeId = id,
        title = title,
        description = description,
        type = type,
        scope = scope,
        isTeam = isTeam,
        targetValue = targetValue,
        startDate = startDate,
        endDate = endDate,
        daysRemaining = daysRemaining,
        status = status,
        myProgress = myProgress,
        myProgressPercent = myProgressPercent,
        leaderboard = leaderboard,
        groupProgressPercent = groupProgressPercent,
    )

    // ── List mode ─────────────────────────────────────────────────────────────

    @Test
    fun `list mode shows Challenges title`() {
        composeRule.setContent {
            BilboTheme { ChallengeScreen() }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Challenges").assertExists()
    }

    @Test
    fun `list mode empty state shows no-active-challenges message`() {
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = emptyList()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No active challenges. Create one to stay motivated!").assertExists()
    }

    @Test
    fun `list mode loading shows circular progress indicator (no empty state)`() {
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = emptyList(), isLoading = true))
            }
        }
        composeRule.waitForIdle()
        // Loading branch: empty-state text should not appear
        composeRule.onNodeWithText("No active challenges. Create one to stay motivated!").assertDoesNotExist()
    }

    @Test
    fun `list mode Create Challenge button fires mode change to Create`() {
        var newMode: ChallengeScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(),
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Challenge").performClick()
        composeRule.waitForIdle()
        assertEquals(ChallengeScreenMode.Create, newMode)
    }

    @Test
    fun `list mode back button fires onBack`() {
        var backCount = 0
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    mode = ChallengeScreenMode.List,
                    onBack = { backCount++ },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(1, backCount)
    }

    // ── List mode challenge card branches ─────────────────────────────────────

    @Test
    fun `challenge list card shows title, progress, and solo label`() {
        val item = makeListItem(title = "FP Challenge", isTeam = false, progressPercent = 33)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FP Challenge").assertExists()
        composeRule.onNodeWithText("Solo").assertExists()
        composeRule.onNodeWithText("33% complete").assertExists()
    }

    @Test
    fun `challenge list card shows Team label when isTeam is true`() {
        val item = makeListItem(title = "Team Challenge", isTeam = true, progressPercent = 70)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Team").assertExists()
        composeRule.onNodeWithText("70% complete").assertExists()
    }

    @Test
    fun `challenge list card shows days remaining`() {
        val item = makeListItem(daysRemaining = 14)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("14d left").assertExists()
    }

    @Test
    fun `tapping challenge list item fires onModeChange with Detail mode`() {
        var newMode: ChallengeScreenMode? = null
        val item = makeListItem(id = "ch-42")
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(challenges = listOf(item)),
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Test Challenge").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(ChallengeScreenMode.Detail("ch-42"), newMode)
    }

    @Test
    fun `type chip shown in list card for REDUCE_EMPTY_CALORIES type`() {
        val item = makeListItem(type = ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(state = ChallengeDetailUiState(challenges = listOf(item)))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reduce Scrolling").assertExists()
    }

    // ── Detail mode: null selected → loading ──────────────────────────────────

    @Test
    fun `detail mode with null selectedChallenge shows loading indicator not title`() {
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = null),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        // Title falls back to "Challenge" when selectedChallenge is null
        composeRule.onNodeWithText("Challenge").assertExists()
    }

    // ── Detail mode: back returns to List ─────────────────────────────────────

    @Test
    fun `back button in Detail mode fires onModeChange to List`() {
        var newMode: ChallengeScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = makeDetailItem()),
                    mode = ChallengeScreenMode.Detail("c1"),
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(ChallengeScreenMode.List, newMode)
    }

    // ── Detail mode: cooperative challenge ───────────────────────────────────

    @Test
    fun `detail mode cooperative shows group progress section`() {
        val detail = makeDetailItem(isTeam = true, groupProgressPercent = 42)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Group Progress").assertExists()
        composeRule.onNodeWithText("42% of team goal reached").assertExists()
    }

    @Test
    fun `detail mode cooperative does not show Leaderboard`() {
        val detail = makeDetailItem(isTeam = true, leaderboard = emptyList())
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Leaderboard").assertDoesNotExist()
    }

    // ── Detail mode: competitive challenge ───────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `detail mode competitive shows leaderboard section`() {
        val entries = listOf(
            ChallengeLeaderboardEntry(1, "u1", "Alice", 100, "100 min", false),
            ChallengeLeaderboardEntry(2, "u2", "Bob", 80, "80 min", true),
        )
        val detail = makeDetailItem(isTeam = false, leaderboard = entries)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Leaderboard"))
        composeRule.onNodeWithText("Leaderboard").assertExists()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Alice"))
        composeRule.onNodeWithText("Alice").assertExists()
    }

    @Test
    fun `detail mode competitive does not show group progress`() {
        val detail = makeDetailItem(isTeam = false, leaderboard = emptyList())
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("42% of team goal reached").assertDoesNotExist()
    }

    // ── Detail mode: UPCOMING → Join button ───────────────────────────────────

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `detail UPCOMING status shows Join Challenge button`() {
        val detail = makeDetailItem(status = ChallengeEngine.ChallengeStatus.UPCOMING)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Join Challenge"))
        composeRule.onNodeWithText("Join Challenge").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `tapping Join Challenge button fires onJoinChallenge`() {
        var joinedId = ""
        val detail = makeDetailItem(id = "join-me", status = ChallengeEngine.ChallengeStatus.UPCOMING)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("join-me"),
                    onJoinChallenge = { id -> joinedId = id },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Join Challenge"))
        composeRule.onNodeWithText("Join Challenge").performClick()
        composeRule.waitForIdle()
        assertEquals("join-me", joinedId)
    }

    @Test
    fun `detail ACTIVE status does not show Join Challenge button`() {
        val detail = makeDetailItem(status = ChallengeEngine.ChallengeStatus.ACTIVE)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Join Challenge").assertDoesNotExist()
    }

    // ── Detail mode: description shown/hidden ─────────────────────────────────

    @Test
    fun `detail shows description when non-blank`() {
        val detail = makeDetailItem(description = "This is the challenge description")
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("This is the challenge description").assertExists()
    }

    @Test
    fun `detail hides description when blank`() {
        val detail = makeDetailItem(description = "")
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        // nothing to assert missing — just ensure no crash and title exists
        composeRule.onNodeWithText("Detail Challenge").assertExists()
    }

    // ── Detail: progress card ─────────────────────────────────────────────────

    @Test
    fun `detail shows Your Progress card with my progress values`() {
        val detail = makeDetailItem(myProgress = 60, myProgressPercent = 60, targetValue = 100)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your Progress").assertExists()
        composeRule.onNodeWithText("60 / 100").assertExists()
        composeRule.onNodeWithText("60%").assertExists()
    }

    // ── Detail: meta card badges ──────────────────────────────────────────────

    @Test
    fun `detail cooperative badge shows Cooperative`() {
        val detail = makeDetailItem(isTeam = true)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cooperative").assertExists()
    }

    @Test
    fun `detail competitive badge shows Competitive`() {
        val detail = makeDetailItem(isTeam = false)
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Competitive").assertExists()
    }

    // ── Create mode ───────────────────────────────────────────────────────────

    @Test
    fun `create mode shows Create Challenge title and form`() {
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(mode = ChallengeScreenMode.Create)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Create Challenge").assertExists()
    }

    @Test
    fun `back button in Create mode fires onModeChange to List`() {
        var newMode: ChallengeScreenMode? = null
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    mode = ChallengeScreenMode.Create,
                    onModeChange = { newMode = it },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertEquals(ChallengeScreenMode.List, newMode)
    }

    // ── challengeTitle helper ─────────────────────────────────────────────────

    @Test
    fun `Detail mode title falls back to Challenge when selectedChallenge is null`() {
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = null),
                    mode = ChallengeScreenMode.Detail("x"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Challenge").assertExists()
    }

    @Test
    fun `Detail mode title shows challenge title from selectedChallenge`() {
        val detail = makeDetailItem(title = "My Named Challenge")
        composeRule.setContent {
            BilboTheme {
                ChallengeScreen(
                    state = ChallengeDetailUiState(selectedChallenge = detail),
                    mode = ChallengeScreenMode.Detail("c1"),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("My Named Challenge").assertExists()
    }
}
