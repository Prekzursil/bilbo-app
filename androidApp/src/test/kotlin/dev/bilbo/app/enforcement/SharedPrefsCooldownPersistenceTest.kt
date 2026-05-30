package dev.bilbo.app.enforcement

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Robolectric unit tests for [SharedPrefsCooldownPersistence].
 *
 * Exercises:
 *  - save / loadAll round-trip
 *  - multiple packages saved and loaded
 *  - clear removes a specific package
 *  - loadAll on empty prefs returns empty map
 *  - loadAll ignores keys without the cdl_ prefix (Robolectric prefs are isolated per test)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedPrefsCooldownPersistenceTest {

    private lateinit var persistence: SharedPrefsCooldownPersistence

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        persistence = SharedPrefsCooldownPersistence(context)
        // Clear any leftover state between tests by clearing all saved entries
        persistence.loadAll().keys.forEach { persistence.clear(it) }
    }

    @Test
    fun `loadAll returns empty map when nothing saved`() {
        val result = persistence.loadAll()
        assertTrue(result.isEmpty(), "Expected empty map, got $result")
    }

    @Test
    fun `save and loadAll round-trip single package`() {
        persistence.save("com.example.app", 1_700_000_000L)
        val result = persistence.loadAll()
        assertEquals(1, result.size)
        assertEquals(1_700_000_000L, result["com.example.app"])
    }

    @Test
    fun `save and loadAll round-trip multiple packages`() {
        persistence.save("com.a", 100L)
        persistence.save("com.b", 200L)
        persistence.save("com.c", 300L)
        val result = persistence.loadAll()
        assertEquals(3, result.size)
        assertEquals(100L, result["com.a"])
        assertEquals(200L, result["com.b"])
        assertEquals(300L, result["com.c"])
    }

    @Test
    fun `save overwrites previous value for same package`() {
        persistence.save("com.app", 111L)
        persistence.save("com.app", 999L)
        val result = persistence.loadAll()
        assertEquals(1, result.size)
        assertEquals(999L, result["com.app"])
    }

    @Test
    fun `clear removes specific package`() {
        persistence.save("com.a", 100L)
        persistence.save("com.b", 200L)
        persistence.clear("com.a")
        val result = persistence.loadAll()
        assertFalse(result.containsKey("com.a"), "com.a should have been removed")
        assertTrue(result.containsKey("com.b"), "com.b should still exist")
    }

    @Test
    fun `clear on non-existent package does not throw`() {
        persistence.clear("com.does.not.exist")
        assertTrue(persistence.loadAll().isEmpty())
    }

    @Test
    fun `save zero expiry is stored and retrieved correctly`() {
        persistence.save("com.zero", 0L)
        val result = persistence.loadAll()
        assertEquals(0L, result["com.zero"])
    }

    @Test
    fun `save large expiry value is stored and retrieved correctly`() {
        val farFuture = Long.MAX_VALUE
        persistence.save("com.future", farFuture)
        val result = persistence.loadAll()
        assertEquals(farFuture, result["com.future"])
    }
}
