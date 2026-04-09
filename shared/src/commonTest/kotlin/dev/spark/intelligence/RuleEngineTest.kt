// RuleEngineTest.kt
// Spark — Unit Tests: RuleEngine
//
// Tests: bypass, cooldown, FP balance check, intent required logic

package dev.spark.intelligence

import kotlin.test.*

// MARK: - Domain types

data class AppContext(
    val packageName: String,
    val fpBalance: Int,
    val isInBypassList: Boolean = false,
    val cooldownExpires: Long = 0L,  // epoch ms; 0 = no cooldown
    val nowMs: Long = 1_000_000L
)

sealed class GatekeeperDecision {
    object Allow         : GatekeeperDecision()
    object RequireIntent : GatekeeperDecision()
    data class Block(val reason: String) : GatekeeperDecision()
    object Cooldown      : GatekeeperDecision()
}

enum class EnforcementMode { SOFT_LOCK, HARD_LOCK, TRACK_ONLY }

// MARK: - RuleEngine under test

class RuleEngine(
    val mode: EnforcementMode = EnforcementMode.SOFT_LOCK,
    val fpCostPerOverride: Int = 10,
    val requireIntentAlways: Boolean = false
) {
    /**
     * Evaluate gatekeeper decision for an app launch.
     *
     * Rules (in order):
     * 1. Bypass list → Allow
     * 2. Track-only mode → RequireIntent (logging only)
     * 3. Active cooldown → Cooldown (hard block)
     * 4. Hard lock → check FP; block if insufficient
     * 5. Intent always required → RequireIntent
     * 6. Soft lock → RequireIntent
     */
    fun evaluate(ctx: AppContext): GatekeeperDecision {
        // Rule 1: bypass list
        if (ctx.isInBypassList) return GatekeeperDecision.Allow

        // Rule 2: track-only
        if (mode == EnforcementMode.TRACK_ONLY) return GatekeeperDecision.RequireIntent

        // Rule 3: active cooldown
        if (ctx.cooldownExpires > ctx.nowMs) return GatekeeperDecision.Cooldown

        // Rule 4: hard lock — gate on FP
        if (mode == EnforcementMode.HARD_LOCK) {
            return if (ctx.fpBalance >= fpCostPerOverride) {
                GatekeeperDecision.RequireIntent
            } else {
                GatekeeperDecision.Block("Insufficient Focus Points. You need $fpCostPerOverride FP to override.")
            }
        }

        // Rule 5: intent always required
        if (requireIntentAlways) return GatekeeperDecision.RequireIntent

        // Rule 6: soft lock default
        return GatekeeperDecision.RequireIntent
    }

    /** Check whether a user has enough FP for an override. */
    fun canAffordOverride(fpBalance: Int): Boolean = fpBalance >= fpCostPerOverride
}

// MARK: - Tests

class RuleEngineTest {

    // ── Bypass list ───────────────────────────────────────────────────────

    @Test
    fun testBypassListAlwaysAllows() {
        val engine = RuleEngine(mode = EnforcementMode.HARD_LOCK)
        val ctx = AppContext(packageName = "com.messages.android", fpBalance = 0, isInBypassList = true)
        assertEquals(GatekeeperDecision.Allow, engine.evaluate(ctx))
    }

    @Test
    fun testBypassListSkipsCooldownCheck() {
        val engine = RuleEngine()
        val ctx = AppContext(
            packageName = "com.phone",
            fpBalance = 100,
            isInBypassList = true,
            cooldownExpires = Long.MAX_VALUE // active cooldown — should be skipped
        )
        assertEquals(GatekeeperDecision.Allow, engine.evaluate(ctx))
    }

    // ── Track-only mode ───────────────────────────────────────────────────

    @Test
    fun testTrackOnlyRequiresIntentButDoesNotBlock() {
        val engine = RuleEngine(mode = EnforcementMode.TRACK_ONLY)
        val ctx = AppContext(packageName = "com.instagram.android", fpBalance = 0)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    @Test
    fun testTrackOnlyIgnoresCooldown() {
        val engine = RuleEngine(mode = EnforcementMode.TRACK_ONLY)
        val ctx = AppContext(packageName = "com.tiktok", fpBalance = 0, cooldownExpires = Long.MAX_VALUE)
        // Cooldown check is skipped in TRACK_ONLY
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    // ── Cooldown ──────────────────────────────────────────────────────────

    @Test
    fun testActiveCooldownBlocksAccess() {
        val engine = RuleEngine(mode = EnforcementMode.SOFT_LOCK)
        val now = 1_000_000L
        val ctx = AppContext(packageName = "com.tiktok", fpBalance = 100, cooldownExpires = now + 60_000, nowMs = now)
        assertEquals(GatekeeperDecision.Cooldown, engine.evaluate(ctx))
    }

    @Test
    fun testExpiredCooldownAllowsIntent() {
        val engine = RuleEngine(mode = EnforcementMode.SOFT_LOCK)
        val now = 1_000_000L
        val ctx = AppContext(packageName = "com.tiktok", fpBalance = 100, cooldownExpires = now - 1, nowMs = now)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    @Test
    fun testNoCooldownAllowsIntent() {
        val engine = RuleEngine(mode = EnforcementMode.SOFT_LOCK)
        val ctx = AppContext(packageName = "com.tiktok", fpBalance = 100, cooldownExpires = 0)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    // ── FP check (Hard Lock) ──────────────────────────────────────────────

    @Test
    fun testHardLockWithSufficientFPRequiresIntent() {
        val engine = RuleEngine(mode = EnforcementMode.HARD_LOCK, fpCostPerOverride = 10)
        val ctx = AppContext(packageName = "com.youtube", fpBalance = 10)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    @Test
    fun testHardLockWithInsufficientFPBlocks() {
        val engine = RuleEngine(mode = EnforcementMode.HARD_LOCK, fpCostPerOverride = 10)
        val ctx = AppContext(packageName = "com.youtube", fpBalance = 5)
        val decision = engine.evaluate(ctx)
        assertTrue(decision is GatekeeperDecision.Block)
    }

    @Test
    fun testHardLockWithZeroFPBlocks() {
        val engine = RuleEngine(mode = EnforcementMode.HARD_LOCK, fpCostPerOverride = 10)
        val ctx = AppContext(packageName = "com.youtube", fpBalance = 0)
        assertTrue(engine.evaluate(ctx) is GatekeeperDecision.Block)
    }

    @Test
    fun testBlockMessageMentionsFPCost() {
        val engine = RuleEngine(mode = EnforcementMode.HARD_LOCK, fpCostPerOverride = 15)
        val ctx = AppContext(packageName = "com.youtube", fpBalance = 0)
        val decision = engine.evaluate(ctx) as GatekeeperDecision.Block
        assertTrue(decision.reason.contains("15"))
    }

    // ── Intent required ───────────────────────────────────────────────────

    @Test
    fun testRequireIntentAlwaysInSoftLock() {
        val engine = RuleEngine(mode = EnforcementMode.SOFT_LOCK, requireIntentAlways = true)
        val ctx = AppContext(packageName = "com.spotify", fpBalance = 100)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    @Test
    fun testSoftLockDefaultRequiresIntent() {
        val engine = RuleEngine(mode = EnforcementMode.SOFT_LOCK)
        val ctx = AppContext(packageName = "com.twitter", fpBalance = 0)
        assertEquals(GatekeeperDecision.RequireIntent, engine.evaluate(ctx))
    }

    // ── canAffordOverride ─────────────────────────────────────────────────

    @Test
    fun testCanAffordOverrideExactCost() {
        val engine = RuleEngine(fpCostPerOverride = 10)
        assertTrue(engine.canAffordOverride(10))
    }

    @Test
    fun testCanAffordOverrideAboveCost() {
        val engine = RuleEngine(fpCostPerOverride = 10)
        assertTrue(engine.canAffordOverride(50))
    }

    @Test
    fun testCannotAffordOverrideBelowCost() {
        val engine = RuleEngine(fpCostPerOverride = 10)
        assertFalse(engine.canAffordOverride(9))
    }

    @Test
    fun testCannotAffordOverrideAtZero() {
        val engine = RuleEngine(fpCostPerOverride = 10)
        assertFalse(engine.canAffordOverride(0))
    }
}
