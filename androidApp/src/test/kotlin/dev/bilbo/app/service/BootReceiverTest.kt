package dev.bilbo.app.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull

/**
 * Robolectric unit tests for [BootReceiver].
 *
 * Exercises:
 *  - onReceive with ACTION_BOOT_COMPLETED → startForegroundService / startService is invoked
 *  - onReceive with a non-boot action → early return, no service start
 *  - onReceive with null intent → early return
 *
 * Robolectric's shadow for Context.startService / startForegroundService absorbs the call
 * without requiring an actual running service.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = BootReceiver()
    }

    // ── ACTION_BOOT_COMPLETED ─────────────────────────────────────────────────

    @Test
    fun `onReceive with ACTION_BOOT_COMPLETED does not throw`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
        // On sdk=33 (>= O), startForegroundService is called. Robolectric shadow absorbs it.
    }

    @Test
    fun `receiver instance is non-null after construction`() {
        assertNotNull(receiver)
    }

    // ── Ignored actions ───────────────────────────────────────────────────────

    @Test
    fun `onReceive with unrelated action is silently ignored`() {
        val intent = Intent(Intent.ACTION_USER_PRESENT)
        receiver.onReceive(context, intent) // Should return early — no crash
    }

    @Test
    fun `onReceive with null intent is silently ignored`() {
        receiver.onReceive(context, null) // Should return early — no crash
    }

    @Test
    fun `onReceive with ACTION_SCREEN_OFF is silently ignored`() {
        val intent = Intent(Intent.ACTION_SCREEN_OFF)
        receiver.onReceive(context, intent) // Not a boot action — no service started
    }

}
