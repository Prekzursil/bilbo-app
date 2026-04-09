package dev.bilbo.enforcement

import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Tracks which apps are currently in a cooldown period after a hard lock enforcement.
 *
 * ### Design
 * - In-memory [MutableMap] is the single source of truth at runtime.
 * - A [CooldownPersistence] adapter (platform-specific) optionally syncs to
 *   SharedPreferences (Android) / UserDefaults (iOS) so state survives process death.
 * - Thread safety is handled by the caller via Kotlin coroutine context — all
 *   public functions should be called from a single coroutine scope or with
 *   appropriate synchronisation.
 *
 * ### Lifecycle
 * Instantiate once (e.g. via DI singleton), call [restoreFromPersistence] on startup,
 * then use [lockApp] / [isLocked] / [getRemainingSeconds] / [unlockApp] as needed.
 */
class CooldownManager(
    private val persistence: CooldownPersistence = NoOpCooldownPersistence,
) {

    /**
     * Internal record holding when a cooldown expires.
     * [expiryEpochSeconds] is a Unix timestamp in seconds.
     */
    private data class CooldownEntry(val expiryEpochSeconds: Long)

    private val cooldowns: MutableMap<String, CooldownEntry> = mutableMapOf()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Put [packageName] into cooldown for [durationMinutes] minutes.
     * If already locked, the existing lock is extended to whichever expiry is later.
     */
    fun lockApp(packageName: String, durationMinutes: Int) {
        val nowSecs = Clock.System.now().epochSeconds
        val newExpiry = nowSecs + durationMinutes.minutes.inWholeSeconds
        val existingExpiry = cooldowns[packageName]?.expiryEpochSeconds ?: 0L
        val finalExpiry = maxOf(newExpiry, existingExpiry)

        cooldowns[packageName] = CooldownEntry(expiryEpochSeconds = finalExpiry)
        persistence.save(packageName, finalExpiry)
    }

    /**
     * Returns true if [packageName] is currently in an active cooldown.
     * Expired entries are cleaned up lazily on each call.
     */
    fun isLocked(packageName: String): Boolean {
        val entry = cooldowns[packageName] ?: return false
        val nowSecs = Clock.System.now().epochSeconds
        if (nowSecs >= entry.expiryEpochSeconds) {
            // Lazily remove expired entry
            cooldowns.remove(packageName)
            persistence.clear(packageName)
            return false
        }
        return true
    }

    /**
     * Returns the remaining cooldown in whole minutes for [packageName],
     * or null if the app is not locked.
     */
    fun getRemainingMinutes(packageName: String): Int? {
        if (!isLocked(packageName)) return null
        val entry = cooldowns[packageName] ?: return null
        val nowSecs = Clock.System.now().epochSeconds
        val remainingSeconds = (entry.expiryEpochSeconds - nowSecs).coerceAtLeast(0L)
        return (remainingSeconds / 60).toInt()
    }

    /**
     * Returns the remaining cooldown in whole seconds for [packageName],
     * or null if the app is not locked.
     * Use this for real-time countdown displays.
     */
    fun getRemainingSeconds(packageName: String): Long? {
        if (!isLocked(packageName)) return null
        val entry = cooldowns[packageName] ?: return null
        val nowSecs = Clock.System.now().epochSeconds
        return (entry.expiryEpochSeconds - nowSecs).coerceAtLeast(0L)
    }

    /**
     * Returns the Unix epoch second at which [packageName]'s cooldown expires,
     * or null if not locked.
     */
    fun getExpiryEpoch(packageName: String): Long? {
        if (!isLocked(packageName)) return null
        return cooldowns[packageName]?.expiryEpochSeconds
    }

    /**
     * Immediately removes the cooldown for [packageName].
     * Used when an override is accepted.
     */
    fun unlockApp(packageName: String) {
        cooldowns.remove(packageName)
        persistence.clear(packageName)
    }

    /**
     * Returns all currently locked package names (expired entries excluded).
     */
    fun getAllLockedApps(): List<String> {
        val nowSecs = Clock.System.now().epochSeconds
        // Purge expired entries
        val expired = cooldowns.entries
            .filter { (_, entry) -> nowSecs >= entry.expiryEpochSeconds }
            .map { it.key }
        expired.forEach { pkg ->
            cooldowns.remove(pkg)
            persistence.clear(pkg)
        }
        return cooldowns.keys.toList()
    }

    /**
     * Load any previously persisted cooldown entries from [persistence].
     * Call once at startup before any [isLocked] checks.
     */
    fun restoreFromPersistence() {
        val saved = persistence.loadAll()
        val nowSecs = Clock.System.now().epochSeconds
        saved.forEach { (pkg, expiry) ->
            if (expiry > nowSecs) {
                cooldowns[pkg] = CooldownEntry(expiryEpochSeconds = expiry)
            } else {
                persistence.clear(pkg)
            }
        }
    }
}

// ── Persistence contract ──────────────────────────────────────────────────────

/**
 * Platform-specific persistence adapter for cooldown state.
 * Android implementation uses SharedPreferences; iOS uses UserDefaults.
 */
interface CooldownPersistence {
    /** Persist [expiryEpochSeconds] for [packageName]. */
    fun save(packageName: String, expiryEpochSeconds: Long)

    /** Remove the persisted entry for [packageName]. */
    fun clear(packageName: String)

    /** Load all persisted entries. Returns a map of packageName → expiryEpochSeconds. */
    fun loadAll(): Map<String, Long>
}

/** No-op implementation — state is in-memory only, does not survive process death. */
object NoOpCooldownPersistence : CooldownPersistence {
    override fun save(packageName: String, expiryEpochSeconds: Long) = Unit
    override fun clear(packageName: String) = Unit
    override fun loadAll(): Map<String, Long> = emptyMap()
}
