package dev.bilbo.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.bilbo.app.ui.theme.BilboTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for the leaderboard rendering helpers in
 * ChallengeLeaderboard.kt. Since [leaderboardSection] is an internal
 * [LazyListScope] extension we exercise it via a [LazyColumn] wrapper.
 *
 * Covers:
 *  - empty leaderboard renders nothing (no "Leaderboard" header)
 *  - populated leaderboard renders header and up to LB_LIMIT (5) rows
 *  - rank 1/2/3 render medal emoji; rank > 3 renders "#N"
 *  - current-user row is bold (semantics verify display-name present)
 *  - rows beyond LB_LIMIT (6+) are truncated to 5
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChallengeLeaderboardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `empty leaderboard renders no header`() {
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(emptyList())
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Leaderboard").assertDoesNotExist()
    }

    @Test
    fun `populated leaderboard renders header and display names`() {
        val entries = listOf(
            ChallengeLeaderboardEntry(1, "u1", "Alice", 100, "100 min", false),
            ChallengeLeaderboardEntry(2, "u2", "Bob", 80, "80 min", false),
            ChallengeLeaderboardEntry(3, "u3", "Carol", 60, "60 min", true),
        )
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(entries)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Leaderboard").assertExists()
        composeRule.onNodeWithText("Alice").assertExists()
        composeRule.onNodeWithText("Bob").assertExists()
        composeRule.onNodeWithText("Carol").assertExists()
    }

    @Test
    fun `medal emojis displayed for top 3 ranks`() {
        val entries = listOf(
            ChallengeLeaderboardEntry(1, "u1", "Gold", 100, "100 pts", false),
            ChallengeLeaderboardEntry(2, "u2", "Silver", 80, "80 pts", false),
            ChallengeLeaderboardEntry(3, "u3", "Bronze", 60, "60 pts", false),
        )
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(entries)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("🥇").assertExists()
        composeRule.onNodeWithText("🥈").assertExists()
        composeRule.onNodeWithText("🥉").assertExists()
    }

    @Test
    fun `rank beyond 3 renders hash-number label`() {
        val entries = listOf(
            ChallengeLeaderboardEntry(1, "u1", "First", 100, "100 pts", false),
            ChallengeLeaderboardEntry(2, "u2", "Second", 90, "90 pts", false),
            ChallengeLeaderboardEntry(3, "u3", "Third", 80, "80 pts", false),
            ChallengeLeaderboardEntry(4, "u4", "Fourth", 70, "70 pts", false),
        )
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(entries)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("#4").assertExists()
    }

    @Test
    fun `only first 5 entries are rendered when list exceeds limit`() {
        val entries = (1..7).map { rank ->
            ChallengeLeaderboardEntry(rank, "u$rank", "Player $rank", 100 - rank * 10, "${100 - rank * 10} pts", false)
        }
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(entries)
                }
            }
        }
        composeRule.waitForIdle()
        // 5th entry should appear
        composeRule.onNodeWithText("Player 5").assertExists()
        // 6th and 7th should not
        composeRule.onNodeWithText("Player 6").assertDoesNotExist()
        composeRule.onNodeWithText("Player 7").assertDoesNotExist()
    }

    @Test
    fun `progress labels are rendered for each row`() {
        val entries = listOf(
            ChallengeLeaderboardEntry(1, "u1", "Alice", 100, "100 nutritive min", false),
            ChallengeLeaderboardEntry(2, "u2", "Bob", 80, "80 nutritive min", true),
        )
        composeRule.setContent {
            BilboTheme {
                LazyColumn {
                    leaderboardSection(entries)
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("100 nutritive min").assertExists()
        composeRule.onNodeWithText("80 nutritive min").assertExists()
    }
}
