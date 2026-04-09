// GamingDetectorTest.kt
// Spark — Unit Tests: GamingDetector
//
// Tests anti-gaming detection rules:
//   • Rapid successive overrides
//   • Short sessions after unlock
//   • Night-time override bursts
//   • Repeated overrides for the same app in a short window
//   • Legitimate use patterns that should NOT trigger

package dev.spark.intelligence

import kotlin.test.*

// MARK: - Domain types

data class OverrideEvent(
    val packageName: String,
    val timestampMs: Long,
    val sessionDurationMs: Long = 0L   // 0 = session not yet ended
)

data class GamingDetectionResult(
    val isGaming: Boolean,
    val reason: String = ""
)

// MARK: - GamingDetector under test

class GamingDetector(
    /** Maximum overrides in [windowMs] before flagging rapid-fire. */
    private val rapidFireThreshold: Int = 3,
    private val rapidFireWindowMs: Long = 5 * 60 * 1000L,  // 5 min

    /** Minimum session duration to be considered legitimate. */
    private val minLegitSessionMs: Long = 60_000L,          // 1 min

    /** Maximum overrides per app in a short window. */
    private val perAppThreshold: Int = 2,
    private val perAppWindowMs: Long = 10 * 60 * 1000L,    // 10 min

    /** Night-time window (hour of day, inclusive). */
    private val nightStartHour: Int = 23,
    private val nightEndHour: Int = 5,
    private val nightBurstThreshold: Int = 2
) {
    /**
     * Analyse a list of recent override events and decide if anti-gaming should fire.
     * Events should be ordered oldest → newest.
     */
    fun analyse(events: List<OverrideEvent>, nowMs: Long = events.lastOrNull()?.timestampMs ?: 0L): GamingDetectionResult {

        if (events.isEmpty()) return GamingDetectionResult(false)

        // Rule 1: rapid successive overrides (global)
        val recentGlobal = events.filter { nowMs - it.timestampMs <= rapidFireWindowMs }
        if (recentGlobal.size >= rapidFireThreshold) {
            return GamingDetectionResult(true, "Rapid successive overrides detected (${recentGlobal.size} in 5 min).")
        }

        // Rule 2: short sessions after unlock (below minimum duration)
        val shortSessions = events.filter {
            it.sessionDurationMs in 1 until minLegitSessionMs
        }
        if (shortSessions.size >= 2) {
            return GamingDetectionResult(true, "Multiple short sessions detected — possible session-skipping.")
        }

        // Rule 3: repeated overrides for the same app
        val byApp = events.groupBy { it.packageName }
        for ((pkg, appEvents) in byApp) {
            val recent = appEvents.filter { nowMs - it.timestampMs <= perAppWindowMs }
            if (recent.size >= perAppThreshold) {
                return GamingDetectionResult(true, "Too many overrides for $pkg in 10 min (${recent.size}).")
            }
        }

        // Rule 4: night-time burst
        val nightOverrides = events.filter { isNightTime(it.timestampMs) }
        if (nightOverrides.size >= nightBurstThreshold) {
            return GamingDetectionResult(true, "Late-night override burst detected.")
        }

        return GamingDetectionResult(false)
    }

    private fun isNightTime(timestampMs: Long): Boolean {
        // Use local hour approximation: UTC hour mod 24
        val hour = ((timestampMs / 3_600_000) % 24).toInt()
        return hour >= nightStartHour || hour < nightEndHour
    }
}

// MARK: - Tests

class GamingDetectorTest {

    private val now = 1_700_000_000_000L // Arbitrary reference time (not night-time hour)

    private fun makeEvent(
        pkg: String = "com.instagram.android",
        offsetMs: Long = 0L,
        durationMs: Long = 0L
    ) = OverrideEvent(pkg, now - offsetMs, durationMs)

    // ── Rapid fire (global) ───────────────────────────────────────────────

    @Test
    fun testRapidFireThreeOverridesInWindow() {
        val events = listOf(
            makeEvent(offsetMs = 60_000),
            makeEvent(offsetMs = 30_000),
            makeEvent(offsetMs = 10_000)
        )
        val result = GamingDetector().analyse(events, now)
        assertTrue(result.isGaming)
        assertTrue(result.reason.contains("Rapid successive overrides"))
    }

    @Test
    fun testTwoOverridesInWindowIsNotFlagged() {
        val events = listOf(
            makeEvent(offsetMs = 60_000),
            makeEvent(offsetMs = 10_000)
        )
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming)
    }

    @Test
    fun testOverridesOutsideWindowNotCounted() {
        val events = listOf(
            makeEvent(offsetMs = 10 * 60_000 + 1), // outside 5-min window
            makeEvent(offsetMs = 60_000),
            makeEvent(offsetMs = 10_000)
        )
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming) // Only 2 within window
    }

    // ── Short sessions ────────────────────────────────────────────────────

    @Test
    fun testTwoShortSessionsTriggersDetection() {
        val events = listOf(
            makeEvent(offsetMs = 120_000, durationMs = 20_000), // 20s — too short
            makeEvent(offsetMs = 60_000,  durationMs = 15_000)  // 15s — too short
        )
        val result = GamingDetector().analyse(events, now)
        assertTrue(result.isGaming)
    }

    @Test
    fun testOneShortSessionDoesNotTrigger() {
        val events = listOf(
            makeEvent(offsetMs = 120_000, durationMs = 20_000),
            makeEvent(offsetMs = 60_000,  durationMs = 180_000) // 3 min — legitimate
        )
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming)
    }

    @Test
    fun testSessionsWithZeroDurationIgnoredByShortSessionRule() {
        // Duration 0 = session still in progress, should not count as short
        val events = listOf(
            makeEvent(offsetMs = 120_000, durationMs = 0),
            makeEvent(offsetMs = 60_000,  durationMs = 0)
        )
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming)
    }

    // ── Per-app threshold ─────────────────────────────────────────────────

    @Test
    fun testTwoOverridesForSameAppInWindowFlagged() {
        val events = listOf(
            makeEvent(pkg = "com.tiktok", offsetMs = 9 * 60_000),
            makeEvent(pkg = "com.tiktok", offsetMs = 1 * 60_000)
        )
        val result = GamingDetector().analyse(events, now)
        assertTrue(result.isGaming)
    }

    @Test
    fun testTwoOverridesDifferentAppsNotFlagged() {
        val events = listOf(
            makeEvent(pkg = "com.instagram.android", offsetMs = 9 * 60_000),
            makeEvent(pkg = "com.tiktok",             offsetMs = 1 * 60_000)
        )
        val result = GamingDetector(rapidFireThreshold = 5).analyse(events, now)
        assertFalse(result.isGaming)
    }

    @Test
    fun testPerAppEventsOutsideWindowNotCounted() {
        val events = listOf(
            makeEvent(pkg = "com.tiktok", offsetMs = 11 * 60_000), // outside 10-min window
            makeEvent(pkg = "com.tiktok", offsetMs = 1 * 60_000)
        )
        // Only 1 within window — should not trigger
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming)
    }

    // ── Night-time burst ──────────────────────────────────────────────────

    @Test
    fun testNightBurstTriggered() {
        // Midnight UTC = hour 0 — inside night window (23–05)
        val midnight = 0L // epoch 0 = midnight UTC
        val events = listOf(
            OverrideEvent("com.instagram.android", midnight + 0),
            OverrideEvent("com.instagram.android", midnight + 60_000)
        )
        val result = GamingDetector(rapidFireThreshold = 10).analyse(events, midnight + 60_000)
        assertTrue(result.isGaming)
    }

    @Test
    fun testDaytimeOverridesNotFlaggedAsNightBurst() {
        // Noon UTC = hour 12 — outside night window
        val noon = 12 * 3_600_000L
        val events = listOf(
            OverrideEvent("com.instagram.android", noon),
            OverrideEvent("com.instagram.android", noon + 60_000)
        )
        val result = GamingDetector(rapidFireThreshold = 10, perAppThreshold = 5).analyse(events, noon + 60_000)
        assertFalse(result.isGaming)
    }

    // ── Empty input ───────────────────────────────────────────────────────

    @Test
    fun testEmptyEventsNotFlagged() {
        val result = GamingDetector().analyse(emptyList())
        assertFalse(result.isGaming)
    }

    // ── Legitimate use ────────────────────────────────────────────────────

    @Test
    fun testLegitimateUsageNotFlagged() {
        val events = listOf(
            OverrideEvent("com.spotify",           now - 3_600_000, durationMs = 1_800_000), // 30 min session
            OverrideEvent("com.instagram.android", now - 1_800_000, durationMs = 600_000)   // 10 min session
        )
        val result = GamingDetector().analyse(events, now)
        assertFalse(result.isGaming)
    }
}
