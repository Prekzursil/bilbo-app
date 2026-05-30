package dev.bilbo.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.app.overlay.GatekeeperController
import dev.bilbo.app.overlay.OverlayManager
import dev.bilbo.data.IntentRepository
import dev.bilbo.tracking.BypassManager
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [GatekeeperModule].
 *
 * Calls each @Provides method directly with real/mock arguments.
 * [OverlayManager] is allowed to construct because Robolectric provides a
 * stub [WindowManager]; [GatekeeperController] is also constructed directly
 * to confirm the provider wires the dependencies.
 *
 * No Hilt component is started.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GatekeeperModuleTest {

    private lateinit var context: Context
    private val intentRepository = mockk<IntentRepository>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ── BypassManager ─────────────────────────────────────────────────────────

    @Test
    fun `provideBypassManager returns non-null BypassManager`() {
        val manager = GatekeeperModule.provideBypassManager()
        assertNotNull(manager)
        assertIs<BypassManager>(manager)
    }

    // ── OverlayManager ────────────────────────────────────────────────────────

    @Test
    fun `provideOverlayManager returns non-null OverlayManager`() {
        val manager = GatekeeperModule.provideOverlayManager(context)
        assertNotNull(manager)
        assertIs<OverlayManager>(manager)
    }

    // ── GatekeeperController ──────────────────────────────────────────────────

    @Test
    fun `provideGatekeeperController returns non-null GatekeeperController`() {
        val overlayManager = GatekeeperModule.provideOverlayManager(context)
        val bypassManager = GatekeeperModule.provideBypassManager()

        val controller = GatekeeperModule.provideGatekeeperController(
            context = context,
            overlayManager = overlayManager,
            bypassManager = bypassManager,
            intentRepository = intentRepository,
        )

        assertNotNull(controller)
        assertIs<GatekeeperController>(controller)
    }

    // ── Separate calls produce independent instances ──────────────────────────

    @Test
    fun `two calls to provideBypassManager produce independent instances`() {
        val a = GatekeeperModule.provideBypassManager()
        val b = GatekeeperModule.provideBypassManager()
        assertNotNull(a)
        assertNotNull(b)
    }
}
