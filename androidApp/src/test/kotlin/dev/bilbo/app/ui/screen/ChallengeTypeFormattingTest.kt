package dev.bilbo.app.ui.screen

import androidx.compose.ui.graphics.Color
import dev.bilbo.social.ChallengeEngine
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for pure-Kotlin helpers in [ChallengeTypeFormatting].
 *
 * Covers every branch of:
 *  - `displayName()` (6 enum values)
 *  - `description()` (6 enum values)
 *  - `unit()` (6 enum values)
 *  - `challengeTypeColor()` (6 enum values)
 *
 * No Android / Compose runtime is required for these pure switch-on-enum
 * functions, so the test is a plain JUnit class.
 */
class ChallengeTypeFormattingTest {
    @Test
    fun `displayName covers every ChallengeType`() {
        val expectations =
            mapOf(
                ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES to "Reduce Scrolling",
                ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES to "Earn Nutritive Time",
                ChallengeEngine.ChallengeType.REACH_FP_BALANCE to "Reach FP Balance",
                ChallengeEngine.ChallengeType.DAILY_STREAK to "Daily Streak",
                ChallengeEngine.ChallengeType.GROUP_FP_POOL to "Group FP Pool",
                ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS to "Analog Activities",
            )
        for ((type, expected) in expectations) {
            assertEquals(expected, type.displayName(), "displayName for $type")
        }
        // Ensure every enum variant is covered.
        assertEquals(ChallengeEngine.ChallengeType.entries.size, expectations.size)
    }

    @Test
    fun `description covers every ChallengeType and is non-empty`() {
        for (type in ChallengeEngine.ChallengeType.entries) {
            val text = type.description()
            assertTrue(text.isNotBlank(), "description for $type")
        }
        assertEquals(
            "Reduce empty-calorie minutes below a target",
            ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES.description(),
        )
        assertEquals(
            "Accumulate nutritive screen time",
            ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES.description(),
        )
        assertEquals(
            "Reach a Focus Points balance",
            ChallengeEngine.ChallengeType.REACH_FP_BALANCE.description(),
        )
        assertEquals(
            "Maintain a consecutive-day streak",
            ChallengeEngine.ChallengeType.DAILY_STREAK.description(),
        )
        assertEquals(
            "Collectively earn Focus Points",
            ChallengeEngine.ChallengeType.GROUP_FP_POOL.description(),
        )
        assertEquals(
            "Complete analog activity suggestions",
            ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS.description(),
        )
    }

    @Test
    fun `unit covers every ChallengeType`() {
        assertEquals("max minutes", ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES.unit())
        assertEquals("minutes", ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES.unit())
        assertEquals("FP", ChallengeEngine.ChallengeType.REACH_FP_BALANCE.unit())
        assertEquals("days", ChallengeEngine.ChallengeType.DAILY_STREAK.unit())
        assertEquals("FP total", ChallengeEngine.ChallengeType.GROUP_FP_POOL.unit())
        assertEquals("activities", ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS.unit())
    }

    @Test
    fun `challengeTypeColor returns a distinct Color for every type`() {
        val colors =
            ChallengeEngine.ChallengeType.entries.map { challengeTypeColor(it) }
        for (c in colors) {
            assertNotNull(c)
        }
        // All six colours should be distinct.
        assertEquals(colors.size, colors.toSet().size, "all colors distinct")
        // Sanity-check one of them.
        assertEquals(
            Color(0xFFF44336),
            challengeTypeColor(ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES),
        )
    }
}
