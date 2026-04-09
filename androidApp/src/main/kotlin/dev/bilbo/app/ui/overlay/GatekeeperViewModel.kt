package dev.bilbo.app.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.tracking.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel backing [GatekeeperScreen].
 *
 * ### Responsibilities
 * - Holds the user's selected [duration] and typed [intention] text.
 * - On "Start": creates an [IntentDeclaration] via [IntentRepository].
 * - Checks whether the current app profile allows bypass so the overlay can be
 *   skipped for whitelisted apps.
 * - Checks if the app is an [AppCategory.EMPTY_CALORIES] type and surfaces a
 *   warning when the FP (Focus Points) balance is low.
 * - Signals [TimerService] via an Intent to start the countdown.
 */
@HiltViewModel
class GatekeeperViewModel @Inject constructor(
    private val intentRepository: IntentRepository,
    private val appProfileRepository: AppProfileRepository,
) : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────────────

    data class UiState(
        val isLoading: Boolean = false,
        val selectedDurationMinutes: Int = 15,
        val intention: String = "",
        val isBypassedApp: Boolean = false,
        val isEmptyCaloriesApp: Boolean = false,
        val activeDeclarationId: Long? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Load profile information for [appInfo] so the UI can show appropriate
     * context (e.g. EMPTY_CALORIES warning).
     */
    fun loadAppProfile(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                val profile = appProfileRepository.getByPackageName(appInfo.packageName)
                _uiState.update { state ->
                    state.copy(
                        isBypassedApp = profile?.isBypassed == true,
                        isEmptyCaloriesApp = profile?.category == AppCategory.EMPTY_CALORIES,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "GatekeeperViewModel: failed to load app profile")
            }
        }
    }

    /** Update the typed intention text. */
    fun onIntentionChanged(text: String) {
        if (text.length > 100) return
        _uiState.update { it.copy(intention = text) }
    }

    /** Update the selected session duration. */
    fun onDurationSelected(minutes: Int) {
        _uiState.update { it.copy(selectedDurationMinutes = minutes) }
    }

    /**
     * Persist the [IntentDeclaration] and signal the timer to start.
     *
     * @param appInfo  The app being opened.
     * @param onTimerStart  Callback invoked with the new declaration ID and
     *                      duration so the service layer can start the timer.
     */
    fun onStart(
        appInfo: AppInfo,
        onTimerStart: (declarationId: Long, durationMinutes: Int) -> Unit,
    ) {
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val declaration = IntentDeclaration(
                    timestamp = Clock.System.now(),
                    declaredApp = appInfo.packageName,
                    declaredDurationMinutes = state.selectedDurationMinutes,
                )
                val id = intentRepository.insert(declaration)
                _uiState.update { it.copy(isLoading = false, activeDeclarationId = id) }
                Timber.d("GatekeeperViewModel: created declaration $id for ${appInfo.packageName}")
                onTimerStart(id, state.selectedDurationMinutes)
            } catch (e: Exception) {
                Timber.e(e, "GatekeeperViewModel: failed to save declaration")
                _uiState.update { it.copy(isLoading = false, error = "Could not save intent. Please try again.") }
            }
        }
    }

    /**
     * Check if there is already an active (non-expired) [IntentDeclaration]
     * for [packageName].  Returns the declaration's remaining seconds if found,
     * or null if none is active.
     */
    fun checkActiveDeclaration(
        packageName: String,
        onResult: (IntentDeclaration?) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val declarations = intentRepository.getByApp(packageName)
                val now = Clock.System.now()
                // Active = most recent declaration whose declared window has not elapsed
                val active = declarations
                    .sortedByDescending { it.timestamp }
                    .firstOrNull { declaration ->
                        val endEpoch = declaration.timestamp.epochSeconds +
                                (declaration.declaredDurationMinutes * 60L)
                        now.epochSeconds < endEpoch
                    }
                onResult(active)
            } catch (e: Exception) {
                Timber.e(e, "GatekeeperViewModel: failed to check active declaration")
                onResult(null)
            }
        }
    }

    /** Clear any transient error message. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
