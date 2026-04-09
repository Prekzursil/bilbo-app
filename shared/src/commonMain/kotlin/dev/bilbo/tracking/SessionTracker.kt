package dev.bilbo.tracking

import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * Tracks app-usage sessions by listening to foreground-app transitions and
 * screen on/off events.
 *
 * ### Session lifecycle
 * 1. When a new foreground app is reported via [onAppChanged], the previous
 *    session (if any) is closed and a new one opened.
 * 2. When the screen turns off ([onScreenOff]), the active session is closed.
 * 3. Sessions shorter than [MIN_SESSION_SECONDS] are discarded.
 *
 * All database writes happen in a supervised coroutine scope so they don't
 * block the caller.
 *
 * @param usageRepository     Persistence layer for [UsageSession] records.
 * @param appProfileRepository Used to look up [AppCategory] for a package.
 */
class SessionTracker(
    private val usageRepository: UsageRepository,
    private val appProfileRepository: AppProfileRepository,
) {

    companion object {
        /** Sessions shorter than this threshold are silently discarded. */
        const val MIN_SESSION_SECONDS = 3L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** ID of the currently-open [UsageSession] row, or null when idle. */
    private var currentSessionId: Long? = null

    /** The [AppInfo] whose session is currently open. */
    private var currentApp: AppInfo? = null

    /** When the current session was started. */
    private var sessionStartTime: Instant? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call whenever the foreground app changes.  Closes the previous session
     * and opens a new one for [newApp].
     */
    fun onAppChanged(newApp: AppInfo) {
        val previous = currentApp
        val startTime = sessionStartTime
        val sessionId = currentSessionId

        if (previous != null && startTime != null && sessionId != null) {
            // Close previous session
            val now = Clock.System.now()
            val durationSeconds = (now - startTime).inWholeSeconds
            scope.launch {
                if (durationSeconds >= MIN_SESSION_SECONDS) {
                    usageRepository.updateEndTime(sessionId, now, durationSeconds)
                } else {
                    // Too short — delete the stub row
                    usageRepository.deleteById(sessionId)
                }
            }
        }

        // Open new session
        val now = Clock.System.now()
        currentApp = newApp
        sessionStartTime = now
        currentSessionId = null

        scope.launch {
            val category = appProfileRepository.getByPackageName(newApp.packageName)
                ?.category
                ?: AppCategory.NEUTRAL

            val session = UsageSession(
                packageName = newApp.packageName,
                appLabel = newApp.appLabel,
                category = category,
                startTime = now,
                endTime = null,
                durationSeconds = 0L,
                wasTracked = true,
            )
            val id = usageRepository.insert(session)
            currentSessionId = id
        }
    }

    /**
     * Call when the screen turns off.  Closes the active session immediately
     * (if one is open).
     */
    fun onScreenOff() {
        val startTime = sessionStartTime ?: return
        val sessionId = currentSessionId ?: return

        val now = Clock.System.now()
        val durationSeconds = (now - startTime).inWholeSeconds

        scope.launch {
            if (durationSeconds >= MIN_SESSION_SECONDS) {
                usageRepository.updateEndTime(sessionId, now, durationSeconds)
            } else {
                usageRepository.deleteById(sessionId)
            }
        }

        currentSessionId = null
        currentApp = null
        sessionStartTime = null
    }

    /**
     * Call when the screen turns back on (user present / unlocked).  The next
     * foreground-app event will open a new session automatically.
     */
    fun onScreenOn() {
        // Reset state so the next onAppChanged creates a fresh session.
        currentSessionId = null
        currentApp = null
        sessionStartTime = null
    }

    /**
     * Closes any open session and stops tracking.  Call when the service is
     * destroyed.
     */
    fun stop() {
        onScreenOff()
    }
}
