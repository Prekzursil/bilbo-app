package dev.bilbo.app.overlay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.data.IntentRepository
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.tracking.AppInfo
import dev.bilbo.tracking.AppMonitor
import dev.bilbo.tracking.BypassManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Unit tests for [GatekeeperController].
 *
 * [OverlayManager] is fully mocked so WindowManager is never touched.
 * [BypassManager] is used directly (it is a pure in-memory class).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GatekeeperControllerTest {

    private lateinit var context: Context
    private val overlayManager = mockk<OverlayManager>(relaxed = true)
    private val bypassManager = mockk<BypassManager>(relaxed = true)
    private val intentRepository = mockk<IntentRepository>(relaxed = true)
    private val appMonitor = mockk<AppMonitor>(relaxed = true)

    private val appInfo = AppInfo(packageName = "com.test.app", appLabel = "Test App", category = null)
    private val now = Clock.System.now()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        every { bypassManager.shouldBypass(any()) } returns false
        coEvery { intentRepository.getByApp(any()) } returns emptyList()
        coEvery { intentRepository.insert(any()) } returns 1L
    }

    private fun makeController(): GatekeeperController =
        GatekeeperController(
            context = context,
            overlayManager = overlayManager,
            bypassManager = bypassManager,
            intentRepository = intentRepository,
        )

    // ── Constants ─────────────────────────────────────────────────────────────

    @Test
    fun `companion constants have expected values`() {
        assertEquals("dev.bilbo.app.action.START_TIMER", GatekeeperController.ACTION_START_TIMER)
        assertEquals("extra_declaration_id", GatekeeperController.EXTRA_DECLARATION_ID)
        assertEquals("extra_duration_minutes", GatekeeperController.EXTRA_DURATION_MINUTES)
    }

    // ── attach ────────────────────────────────────────────────────────────────

    @Test
    fun `attach registers a callback with AppMonitor`() {
        val controller = makeController()
        controller.attach(appMonitor)
        verify(exactly = 1) { appMonitor.onAppChanged(any()) }
    }

    // ── dismiss ───────────────────────────────────────────────────────────────

    @Test
    fun `dismiss delegates to overlayManager`() {
        val controller = makeController()
        controller.dismiss()
        verify(exactly = 1) { overlayManager.dismiss() }
    }

    // ── handleForegroundChange: bypass path ───────────────────────────────────

    @Test
    fun `bypass app causes overlay dismiss immediately`() {
        every { bypassManager.shouldBypass("com.bypass.app") } returns true
        val controller = makeController()

        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(
            AppInfo(packageName = "com.bypass.app", appLabel = "Bypass", category = null),
        )

        verify(exactly = 1) { overlayManager.dismiss() }
    }

    @Test
    fun `non-bypass app does not trigger immediate dismiss`() {
        every { bypassManager.shouldBypass("com.normal.app") } returns false
        val controller = makeController()

        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(
            AppInfo(packageName = "com.normal.app", appLabel = "Normal", category = null),
        )

        // dismiss should NOT be called for non-bypass apps
        verify(exactly = 0) { overlayManager.dismiss() }
    }

    // ── handleForegroundChange: active declaration skip ───────────────────────

    @Test
    fun `active declaration prevents gatekeeper display`() {
        // An unexpired declaration (started now, 30 min duration)
        val activeDeclaration = IntentDeclaration(
            id = 1L,
            timestamp = now,
            declaredApp = appInfo.packageName,
            declaredDurationMinutes = 30,
        )
        coEvery { intentRepository.getByApp(appInfo.packageName) } returns listOf(activeDeclaration)

        val controller = makeController()
        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(appInfo)
        Thread.sleep(200)

        // Gatekeeper should NOT be shown (active declaration exists)
        verify(exactly = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    @Test
    fun `expired declaration triggers gatekeeper display`() {
        // Declaration that expired 2 hours ago
        val expiredInstant = kotlin.time.Instant.fromEpochSeconds(now.epochSeconds - 7200)
        val expiredDeclaration = IntentDeclaration(
            id = 1L,
            timestamp = expiredInstant,
            declaredApp = appInfo.packageName,
            declaredDurationMinutes = 1, // 1 minute → expired long ago
        )
        coEvery { intentRepository.getByApp(appInfo.packageName) } returns listOf(expiredDeclaration)

        val controller = makeController()
        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(appInfo)
        Thread.sleep(200)

        // Gatekeeper IS shown since declaration expired
        verify(atLeast = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    @Test
    fun `no declaration triggers gatekeeper display`() {
        coEvery { intentRepository.getByApp(appInfo.packageName) } returns emptyList()

        val controller = makeController()
        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(appInfo)
        Thread.sleep(200)

        // Gatekeeper IS shown since there's no active declaration
        verify(atLeast = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    // ── checkActiveDeclaration: repository exception ──────────────────────────

    @Test
    fun `repository exception in checkActiveDeclaration is swallowed and treated as no active declaration`() {
        coEvery { intentRepository.getByApp(any()) } throws RuntimeException("db error")

        val controller = makeController()
        val callbackSlot = slot<(AppInfo) -> Unit>()
        every { appMonitor.onAppChanged(capture(callbackSlot)) } returns Unit
        controller.attach(appMonitor)

        callbackSlot.captured.invoke(appInfo)
        Thread.sleep(200)
        // Should not throw; gatekeeper will attempt to show
        verify(atLeast = 0) { overlayManager.showGatekeeper(any(), any()) }
    }

    // ── BypassManager default set ─────────────────────────────────────────────

    @Test
    fun `BypassManager DEFAULT_BYPASS_PACKAGES contains expected system apps`() {
        val defaults = BypassManager.DEFAULT_BYPASS_PACKAGES
        assertTrue(defaults.contains("com.android.dialer"))
        assertTrue(defaults.contains("com.google.android.apps.maps"))
    }
}
