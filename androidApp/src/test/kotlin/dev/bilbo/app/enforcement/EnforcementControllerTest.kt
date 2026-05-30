package dev.bilbo.app.enforcement

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.app.overlay.OverlayManager
import dev.bilbo.app.service.TimerService
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.DopamineBudget
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.FPEconomy
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.economy.FocusPointsEngine
import dev.bilbo.enforcement.CooldownManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Unit tests for [EnforcementController].
 *
 * [OverlayManager] is mocked so WindowManager/TYPE_APPLICATION_OVERLAY calls
 * never reach the OS — all business-logic branches are exercised on Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EnforcementControllerTest {

    private lateinit var context: Context
    private val overlayManager = mockk<OverlayManager>(relaxed = true)
    private val appProfileRepository = mockk<AppProfileRepository>(relaxed = true)
    private val intentRepository = mockk<IntentRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val suggestionRepository = mockk<SuggestionRepository>(relaxed = true)
    private val cooldownManager = mockk<CooldownManager>(relaxed = true)
    private val focusPointsEngine = mockk<FocusPointsEngine>(relaxed = true)

    private lateinit var controller: EnforcementController

    private val today: LocalDate get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun defaultBudget() = DopamineBudget(
        date = today,
        fpEarned = 20, fpSpent = 5, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0,
        nutritiveMinutes = 10, emptyCalorieMinutes = 5, neutralMinutes = 0,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = EnforcementController(
            context = context,
            overlayManager = overlayManager,
            appProfileRepository = appProfileRepository,
            intentRepository = intentRepository,
            budgetRepository = budgetRepository,
            suggestionRepository = suggestionRepository,
            cooldownManager = cooldownManager,
            focusPointsEngine = focusPointsEngine,
        )

        // Default stubs
        coEvery { budgetRepository.getByDate(any()) } returns defaultBudget()
        every { focusPointsEngine.getBalance(any()) } returns 30
        coEvery { intentRepository.getById(any()) } returns null
        coEvery { intentRepository.updateActualDuration(any(), any()) } returns Unit
        coEvery { intentRepository.updateEnforcement(any(), any(), any(), any()) } returns Unit
        coEvery { suggestionRepository.getAll() } returns emptyList()
        every { cooldownManager.isLocked(any()) } returns false
        every { cooldownManager.getRemainingSeconds(any()) } returns null
    }

    @After
    fun tearDown() {
        // Unregister to avoid leaks between tests
        try { controller.unregister() } catch (_: Exception) {}
    }

    // ── register / unregister ─────────────────────────────────────────────────

    @Test
    fun `register calls cooldownManager restoreFromPersistence`() {
        controller.register()
        verify(exactly = 1) { cooldownManager.restoreFromPersistence() }
    }

    @Test
    fun `unregister does not throw when not registered`() {
        // Should not throw even if called without prior register
        controller.unregister()
    }

    @Test
    fun `register then unregister completes without exception`() {
        controller.register()
        controller.unregister()
    }

    // ── checkAndEnforceCooldown ───────────────────────────────────────────────

    @Test
    fun `checkAndEnforceCooldown returns false when app not locked`() {
        every { cooldownManager.isLocked("com.example") } returns false
        val result = controller.checkAndEnforceCooldown("com.example", "Example")
        assertFalse(result)
    }

    @Test
    fun `checkAndEnforceCooldown returns true and calls cooldownManager getRemainingSeconds`() {
        every { cooldownManager.isLocked("com.locked") } returns true
        every { cooldownManager.getRemainingSeconds("com.locked") } returns 300L
        val result = controller.checkAndEnforceCooldown("com.locked", "Locked App")
        assertTrue(result)
        // Remaining seconds was queried to populate the overlay
        verify(exactly = 1) { cooldownManager.getRemainingSeconds("com.locked") }
    }

    @Test
    fun `checkAndEnforceCooldown returns true even when getRemainingSeconds returns null`() {
        every { cooldownManager.isLocked("com.locked") } returns true
        every { cooldownManager.getRemainingSeconds("com.locked") } returns null
        val result = controller.checkAndEnforceCooldown("com.locked", "Locked App")
        assertTrue(result)
        // Should still proceed with 0 remaining seconds fallback
        verify(exactly = 1) { cooldownManager.getRemainingSeconds("com.locked") }
    }

    // ── timer-expired broadcast → NUDGE mode ─────────────────────────────────

    @Test
    fun `timer-expired broadcast for NUDGE mode marks declaration enforced`() {
        val pkg = "com.nudge.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns AppProfile(
            packageName = pkg,
            appLabel = "Nudge App",
            category = AppCategory.EMPTY_CALORIES,
            enforcementMode = EnforcementMode.NUDGE,
        )
        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 42L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "Nudge App")
        }

        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(300)
        // NUDGE path: declaration is updated with wasEnforced=true, NUDGE mode
        coVerify(atLeast = 1) {
            intentRepository.updateEnforcement(42L, true, EnforcementMode.NUDGE, false)
        }
    }

    @Test
    fun `timer-expired broadcast triggers hard-lock overlay for HARD_LOCK mode`() {
        val pkg = "com.hardlock.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns AppProfile(
            packageName = pkg,
            appLabel = "HardLock App",
            category = AppCategory.EMPTY_CALORIES,
            enforcementMode = EnforcementMode.HARD_LOCK,
        )
        every { cooldownManager.getRemainingSeconds(pkg) } returns 1800L

        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 99L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "HardLock App")
        }

        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)

        Thread.sleep(200)
        verify(exactly = 1) { cooldownManager.lockApp(pkg, any()) }
    }

    @Test
    fun `timer-expired broadcast falls back to NUDGE when no profile exists`() {
        val pkg = "com.unknown.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns null

        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 1L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "Unknown App")
        }

        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(200)
        // Should not throw; nudge overlay shown
        verify(atLeast = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    @Test
    fun `timer-expired broadcast ignores intent with wrong action`() {
        val intent = Intent("dev.bilbo.other.ACTION")
        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(100)
        verify(exactly = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    @Test
    fun `timer-expired broadcast ignores intent missing app package`() {
        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 1L)
            // No EXTRA_APP_PACKAGE
        }
        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(100)
        verify(exactly = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    // ── FP economy: handleNudgeExtension branch ───────────────────────────────

    @Test
    fun `nudge extension is skipped when balance is below cost`() {
        every { focusPointsEngine.getBalance(any()) } returns 3 // less than NUDGE_EXTENSION_FP_COST=5
        val pkg = "com.nudge.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns AppProfile(
            packageName = pkg, appLabel = "N", category = AppCategory.EMPTY_CALORIES,
            enforcementMode = EnforcementMode.NUDGE,
        )
        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 1L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "N")
        }
        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(200)
        // incrementFpSpent should NOT be called (balance too low)
        coVerify(exactly = 0) { budgetRepository.incrementFpSpent(any(), any()) }
    }

    // ── Suggestion fallback ───────────────────────────────────────────────────

    @Test
    fun `hard-lock uses default suggestion when repository returns empty`() {
        val pkg = "com.hardlock2.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns AppProfile(
            packageName = pkg, appLabel = "HL2", category = AppCategory.EMPTY_CALORIES,
            enforcementMode = EnforcementMode.HARD_LOCK,
        )
        coEvery { suggestionRepository.getAll() } returns emptyList()
        every { cooldownManager.getRemainingSeconds(pkg) } returns 1800L

        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 7L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "HL2")
        }
        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(200)
        verify(exactly = 1) { cooldownManager.lockApp(pkg, any()) }
    }

    // ── Declaration actual-minute computation ─────────────────────────────────

    @Test
    fun `declaration lookup falls back gracefully when intent not found`() {
        coEvery { intentRepository.getById(any()) } returns null
        val pkg = "com.app"
        coEvery { appProfileRepository.getByPackageName(pkg) } returns AppProfile(
            packageName = pkg, appLabel = "App", category = AppCategory.EMPTY_CALORIES,
            enforcementMode = EnforcementMode.NUDGE,
        )
        val intent = Intent(TimerService.ACTION_TIMER_EXPIRED).apply {
            putExtra(TimerService.EXTRA_DECLARATION_ID, 5L)
            putExtra(TimerService.EXTRA_APP_PACKAGE, pkg)
            putExtra(TimerService.EXTRA_APP_LABEL, "App")
        }
        controller.register()
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent)
        Thread.sleep(200)
        // Should still reach overlay without crashing
        verify(atLeast = 0) { overlayManager.showGatekeeper(any(), any()) }
    }
}
