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
 * Robolectric unit tests for [UnlockReceiver].
 *
 * Exercises all three branches in [UnlockReceiver.onReceive]:
 *  - ACTION_USER_PRESENT → startForegroundService / startService called
 *  - ACTION_SCREEN_OFF   → forwardToService called (startService with SCREEN_OFF action)
 *  - Unrecognised action → early-return (no crash)
 *  - null intent         → early-return (no crash)
 *
 * Robolectric's shadow for Context.startService / startForegroundService absorbs calls
 * without a real running service.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnlockReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: UnlockReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = UnlockReceiver()
    }

    // ── Receiver construction ─────────────────────────────────────────────────

    @Test
    fun `receiver is non-null after construction`() {
        assertNotNull(receiver)
    }

    // ── ACTION_USER_PRESENT ───────────────────────────────────────────────────

    @Test
    fun `onReceive with ACTION_USER_PRESENT does not throw`() {
        val intent = Intent(Intent.ACTION_USER_PRESENT)
        receiver.onReceive(context, intent)
        // On sdk=33 (>= O), startForegroundService is called via shadow.
    }

    // ── ACTION_SCREEN_OFF ─────────────────────────────────────────────────────

    @Test
    fun `onReceive with ACTION_SCREEN_OFF does not throw`() {
        val intent = Intent(Intent.ACTION_SCREEN_OFF)
        receiver.onReceive(context, intent)
        // forwardToService path: startService with ACTION_SCREEN_OFF action.
    }

    // ── Unknown / null actions ────────────────────────────────────────────────

    @Test
    fun `onReceive with unrelated action is silently ignored`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent) // Neither USER_PRESENT nor SCREEN_OFF — no-op
    }

    @Test
    fun `onReceive with null intent is silently ignored`() {
        receiver.onReceive(context, null)
    }

    // ── startService exception handling in forwardToService ───────────────────

    @Test
    fun `screen-off path swallows service start exceptions without propagating`() {
        // The catch block in forwardToService swallows all exceptions.
        // Robolectric's shadow doesn't throw, but this test documents
        // that the path is guarded.
        val intent = Intent(Intent.ACTION_SCREEN_OFF)
        receiver.onReceive(context, intent)
    }
}
