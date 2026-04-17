package dev.bilbo.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel backing [DashboardScreen].
 *
 * ### Responsibilities
 * - Observe every [UsageSession] persisted by [dev.bilbo.app.service.UsageTrackingService]
 *   through [UsageRepository.observeAll].
 * - Filter to "today" in the user's local timezone (midnight → now).
 * - Aggregate per-package totals, resolve human-readable labels + categories via
 *   [AppProfileRepository], and expose everything as a [DashboardUiState].
 * - Expose a [refresh] entry point for pull-to-refresh (forces a re-collection).
 *
 * All heavy lifting runs on the [viewModelScope]; the UI reads [uiState] as a cold
 * [StateFlow] so re-subscribes (e.g. after process death) receive the last value.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appProfileRepository: AppProfileRepository,
) : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────────────

    data class AppUsage(
        val packageName: String,
        val appLabel: String,
        val durationMinutes: Int,
        val category: AppCategory,
    )

    data class DashboardUiState(
        val isLoading: Boolean = true,
        val totalMinutes: Int = 0,
        val dailyGoalMinutes: Int = DEFAULT_DAILY_GOAL_MINUTES,
        val apps: List<AppUsage> = emptyList(),
        val error: String? = null,
    ) {
        val goalDeltaCopy: String
            get() {
                val delta = dailyGoalMinutes - totalMinutes
                return when {
                    delta >= 0 -> "${delta} min under your daily goal"
                    else -> "${-delta} min over your daily goal"
                }
            }

        val formattedTotal: String
            get() {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                return if (h > 0) "${h}h ${m}m" else "${m}m"
            }
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        startObserving()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Force a refresh of the dashboard. [UsageRepository.observeAll] already
     * re-emits on every write, so this primarily gives users pull-to-refresh
     * feedback while we re-aggregate from the current snapshot.
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                val sessions = usageRepository.getAll()
                aggregateAndEmit(sessions)
            } catch (e: Exception) {
                Timber.e(e, "DashboardViewModel: refresh failed")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not refresh dashboard",
                )
            }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun startObserving() {
        usageRepository.observeAll()
            .onEach { aggregateAndEmit(it) }
            .catch { e ->
                Timber.e(e, "DashboardViewModel: observeAll stream error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not load usage data",
                )
            }
            .launchIn(viewModelScope)
    }

    private suspend fun aggregateAndEmit(sessions: List<UsageSession>) {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today: LocalDate = now.toLocalDateTime(tz).date
        val startOfToday = today.atStartOfDayIn(tz)

        // Today's sessions only
        val todays = sessions.filter { it.startTime >= startOfToday }

        // Aggregate per package: sum durations, resolve label + category
        val perPackage = todays.groupBy { it.packageName }
        val apps = perPackage.map { (pkg, group) ->
            val totalSeconds = group.sumOf { it.durationSeconds }
            val sessionLabel = group.firstOrNull { !it.appLabel.isBlank() }?.appLabel ?: pkg
            val sessionCategory = group.firstOrNull()?.category ?: AppCategory.NEUTRAL

            // Prefer the AppProfile category if one has been set by the user
            val profile = try {
                appProfileRepository.getByPackageName(pkg)
            } catch (e: Exception) {
                Timber.w(e, "DashboardViewModel: profile lookup failed for $pkg")
                null
            }
            AppUsage(
                packageName = pkg,
                appLabel = profile?.let { prof -> prof.appLabel.ifBlank { sessionLabel } }
                    ?: sessionLabel,
                durationMinutes = (totalSeconds / 60L).toInt(),
                category = profile?.category ?: sessionCategory,
            )
        }
            .filter { it.durationMinutes > 0 }
            .sortedByDescending { it.durationMinutes }

        val totalMinutes = apps.sumOf { it.durationMinutes }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            totalMinutes = totalMinutes,
            apps = apps,
            error = null,
        )
    }

    companion object {
        /** Default daily screen-time goal; will be user-configurable in a later release. */
        const val DEFAULT_DAILY_GOAL_MINUTES = 150
    }
}
