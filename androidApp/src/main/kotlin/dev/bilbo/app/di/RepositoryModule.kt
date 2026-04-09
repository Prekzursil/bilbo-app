package dev.bilbo.app.di

import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bilbo.app.emotional.EmotionalFlowSettings
import dev.bilbo.app.enforcement.SharedPrefsCooldownPersistence
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.DopamineBudget
import dev.bilbo.domain.Emotion
import dev.bilbo.domain.EmotionalCheckIn
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay
import dev.bilbo.domain.UsageSession
import dev.bilbo.enforcement.CooldownPersistence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Singleton

/**
 * Provides repository bindings, [CooldownPersistence], [NotificationManager],
 * and [EmotionalFlowSettings].
 *
 * Repository implementations are in-memory stubs that return empty/default data.
 * Replace these with real database-backed implementations when ready.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ── Repository bindings ──────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUsageRepository(): UsageRepository = InMemoryUsageRepository()

    @Provides
    @Singleton
    fun provideAppProfileRepository(): AppProfileRepository = InMemoryAppProfileRepository()

    @Provides
    @Singleton
    fun provideIntentRepository(): IntentRepository = InMemoryIntentRepository()

    @Provides
    @Singleton
    fun provideEmotionRepository(): EmotionRepository = InMemoryEmotionRepository()

    @Provides
    @Singleton
    fun provideBudgetRepository(): BudgetRepository = InMemoryBudgetRepository()

    @Provides
    @Singleton
    fun provideSuggestionRepository(): SuggestionRepository = InMemorySuggestionRepository()

    // ── CooldownPersistence ──────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideCooldownPersistence(
        impl: SharedPrefsCooldownPersistence,
    ): CooldownPersistence = impl

    // ── NotificationManager ──────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
    ): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ── EmotionalFlowSettings ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideEmotionalFlowSettings(): EmotionalFlowSettings = EmotionalFlowSettings()
}

// ═════════════════════════════════════════════════════════════════════════════
// In-memory stub implementations
// ═════════════════════════════════════════════════════════════════════════════

// ── UsageRepository ──────────────────────────────────────────────────────────

private class InMemoryUsageRepository : UsageRepository {

    private val sessions = mutableListOf<UsageSession>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<UsageSession>> = flowOf(sessions.toList())

    override suspend fun getAll(): List<UsageSession> = sessions.toList()

    override suspend fun getById(id: Long): UsageSession? =
        sessions.firstOrNull { it.id == id }

    override suspend fun getByPackageName(packageName: String): List<UsageSession> =
        sessions.filter { it.packageName == packageName }

    override suspend fun getByDateRange(from: Instant, to: Instant): List<UsageSession> =
        sessions.filter { it.startTime in from..to }

    override suspend fun getByCategory(category: AppCategory): List<UsageSession> =
        sessions.filter { it.category == category }

    override suspend fun getByDateRangeAndCategory(
        from: Instant,
        to: Instant,
        category: AppCategory,
    ): List<UsageSession> =
        sessions.filter { it.startTime in from..to && it.category == category }

    override suspend fun insert(session: UsageSession): Long {
        val id = nextId++
        sessions += session.copy(id = id)
        return id
    }

    override suspend fun updateEndTime(id: Long, endTime: Instant, durationSeconds: Long) {
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx >= 0) {
            sessions[idx] = sessions[idx].copy(endTime = endTime, durationSeconds = durationSeconds)
        }
    }

    override suspend fun deleteById(id: Long) {
        sessions.removeAll { it.id == id }
    }

    override suspend fun deleteOlderThan(before: Instant) {
        sessions.removeAll { it.startTime < before }
    }

    override suspend fun countByPackageName(packageName: String): Long =
        sessions.count { it.packageName == packageName }.toLong()

    override suspend fun sumDurationByCategory(from: Instant, to: Instant): Map<AppCategory, Long> =
        sessions
            .filter { it.startTime in from..to }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.durationSeconds } }
}

// ── AppProfileRepository ─────────────────────────────────────────────────────

private class InMemoryAppProfileRepository : AppProfileRepository {

    private val profiles = mutableMapOf<String, AppProfile>()

    override fun observeAll(): Flow<List<AppProfile>> = flowOf(profiles.values.toList())

    override suspend fun getAll(): List<AppProfile> = profiles.values.toList()

    override suspend fun getByPackageName(packageName: String): AppProfile? =
        profiles[packageName]

    override fun observeByPackageName(packageName: String): Flow<AppProfile?> =
        flowOf(profiles[packageName])

    override suspend fun getByCategory(category: AppCategory): List<AppProfile> =
        profiles.values.filter { it.category == category }

    override suspend fun getByEnforcementMode(enforcementMode: EnforcementMode): List<AppProfile> =
        profiles.values.filter { it.enforcementMode == enforcementMode }

    override suspend fun getBypassed(): List<AppProfile> =
        profiles.values.filter { it.isBypassed }

    override suspend fun getCustomClassified(): List<AppProfile> =
        profiles.values.filter { it.isCustomClassification }

    override suspend fun insert(profile: AppProfile) {
        require(!profiles.containsKey(profile.packageName)) {
            "Profile already exists for ${profile.packageName}"
        }
        profiles[profile.packageName] = profile
    }

    override suspend fun update(profile: AppProfile) {
        profiles[profile.packageName] = profile
    }

    override suspend fun upsert(profile: AppProfile) {
        profiles[profile.packageName] = profile
    }

    override suspend fun updateCategory(packageName: String, category: AppCategory) {
        profiles[packageName]?.let {
            profiles[packageName] = it.copy(category = category, isCustomClassification = true)
        }
    }

    override suspend fun updateBypass(packageName: String, isBypassed: Boolean) {
        profiles[packageName]?.let {
            profiles[packageName] = it.copy(isBypassed = isBypassed)
        }
    }

    override suspend fun deleteByPackageName(packageName: String) {
        profiles.remove(packageName)
    }
}

// ── IntentRepository ─────────────────────────────────────────────────────────

private class InMemoryIntentRepository : IntentRepository {

    private val declarations = mutableListOf<IntentDeclaration>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<IntentDeclaration>> = flowOf(declarations.toList())

    override suspend fun getAll(): List<IntentDeclaration> = declarations.toList()

    override suspend fun getById(id: Long): IntentDeclaration? =
        declarations.firstOrNull { it.id == id }

    override suspend fun getByApp(packageName: String): List<IntentDeclaration> =
        declarations.filter { it.declaredApp == packageName }

    override suspend fun getByDateRange(from: Instant, to: Instant): List<IntentDeclaration> =
        declarations.filter { it.timestamp in from..to }

    override suspend fun getOverridden(): List<IntentDeclaration> =
        declarations.filter { it.wasOverridden }

    override suspend fun insert(declaration: IntentDeclaration): Long {
        val id = nextId++
        declarations += declaration.copy(id = id)
        return id
    }

    override suspend fun updateActualDuration(id: Long, actualDurationMinutes: Int) {
        val idx = declarations.indexOfFirst { it.id == id }
        if (idx >= 0) {
            declarations[idx] = declarations[idx].copy(actualDurationMinutes = actualDurationMinutes)
        }
    }

    override suspend fun updateEnforcement(
        id: Long,
        wasEnforced: Boolean,
        enforcementType: EnforcementMode?,
        wasOverridden: Boolean,
    ) {
        val idx = declarations.indexOfFirst { it.id == id }
        if (idx >= 0) {
            declarations[idx] = declarations[idx].copy(
                wasEnforced = wasEnforced,
                enforcementType = enforcementType,
                wasOverridden = wasOverridden,
            )
        }
    }

    override suspend fun deleteById(id: Long) {
        declarations.removeAll { it.id == id }
    }

    override suspend fun countAccurate(from: Instant, to: Instant): Long {
        return declarations
            .filter { it.timestamp in from..to }
            .count { intent ->
                val actual = intent.actualDurationMinutes ?: return@count false
                val declared = intent.declaredDurationMinutes
                if (declared == 0) return@count false
                val delta = kotlin.math.abs(actual - declared).toDouble() / declared
                delta <= 0.20
            }
            .toLong()
    }

    override suspend fun countTotal(from: Instant, to: Instant): Long =
        declarations.count { it.timestamp in from..to }.toLong()
}

// ── EmotionRepository ────────────────────────────────────────────────────────

private class InMemoryEmotionRepository : EmotionRepository {

    private val checkIns = mutableListOf<EmotionalCheckIn>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<EmotionalCheckIn>> = flowOf(checkIns.toList())

    override suspend fun getAll(): List<EmotionalCheckIn> = checkIns.toList()

    override suspend fun getById(id: Long): EmotionalCheckIn? =
        checkIns.firstOrNull { it.id == id }

    override suspend fun getByDateRange(from: Instant, to: Instant): List<EmotionalCheckIn> =
        checkIns.filter { it.timestamp in from..to }

    override suspend fun getByIntentId(intentId: Long): EmotionalCheckIn? =
        checkIns.firstOrNull { it.linkedIntentId == intentId }

    override suspend fun insert(checkIn: EmotionalCheckIn): Long {
        val id = nextId++
        checkIns += checkIn.copy(id = id)
        return id
    }

    override suspend fun updatePostMood(id: Long, postMood: Emotion) {
        val idx = checkIns.indexOfFirst { it.id == id }
        if (idx >= 0) {
            checkIns[idx] = checkIns[idx].copy(postSessionMood = postMood)
        }
    }

    override suspend fun deleteById(id: Long) {
        checkIns.removeAll { it.id == id }
    }
}

// ── BudgetRepository ─────────────────────────────────────────────────────────

private class InMemoryBudgetRepository : BudgetRepository {

    private val budgets = mutableMapOf<LocalDate, DopamineBudget>()

    override fun observeAll(): Flow<List<DopamineBudget>> =
        flowOf(budgets.values.sortedByDescending { it.date })

    override suspend fun getAll(): List<DopamineBudget> =
        budgets.values.sortedByDescending { it.date }

    override suspend fun getByDate(date: LocalDate): DopamineBudget? = budgets[date]

    override fun observeByDate(date: LocalDate): Flow<DopamineBudget?> =
        flowOf(budgets[date])

    override suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<DopamineBudget> =
        budgets.values.filter { it.date in from..to }.sortedBy { it.date }

    override suspend fun getRecent(limit: Long): List<DopamineBudget> =
        budgets.values.sortedByDescending { it.date }.take(limit.toInt())

    override suspend fun insert(budget: DopamineBudget) {
        budgets[budget.date] = budget
    }

    override suspend fun update(budget: DopamineBudget) {
        budgets[budget.date] = budget
    }

    override suspend fun upsert(budget: DopamineBudget) {
        budgets[budget.date] = budget
    }

    override suspend fun incrementFpEarned(date: LocalDate, amount: Int) {
        budgets[date] = (budgets[date] ?: defaultBudget(date)).let {
            it.copy(fpEarned = it.fpEarned + amount)
        }
    }

    override suspend fun incrementFpSpent(date: LocalDate, amount: Int) {
        budgets[date] = (budgets[date] ?: defaultBudget(date)).let {
            it.copy(fpSpent = it.fpSpent + amount)
        }
    }

    override suspend fun incrementFpBonus(date: LocalDate, amount: Int) {
        budgets[date] = (budgets[date] ?: defaultBudget(date)).let {
            it.copy(fpBonus = it.fpBonus + amount)
        }
    }

    override suspend fun deleteByDate(date: LocalDate) {
        budgets.remove(date)
    }

    override suspend fun sumFpEarned(from: LocalDate, to: LocalDate): Long =
        budgets.values
            .filter { it.date in from..to }
            .sumOf { it.fpEarned.toLong() }

    private fun defaultBudget(date: LocalDate) = DopamineBudget(
        date = date,
        fpEarned = 0,
        fpSpent = 0,
        fpBonus = 0,
        fpRolloverIn = 0,
        fpRolloverOut = 0,
        nutritiveMinutes = 0,
        emptyCalorieMinutes = 0,
        neutralMinutes = 0,
    )
}

// ── SuggestionRepository ─────────────────────────────────────────────────────

private class InMemorySuggestionRepository : SuggestionRepository {

    private val suggestions = mutableListOf<AnalogSuggestion>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<AnalogSuggestion>> = flowOf(suggestions.toList())

    override suspend fun getAll(): List<AnalogSuggestion> = suggestions.toList()

    override suspend fun getById(id: Long): AnalogSuggestion? =
        suggestions.firstOrNull { it.id == id }

    override suspend fun getByCategory(category: SuggestionCategory): List<AnalogSuggestion> =
        suggestions
            .filter { it.category == category }
            .sortedByDescending { it.timesAccepted }

    override suspend fun getByTimeOfDay(timeOfDay: TimeOfDay): List<AnalogSuggestion> =
        suggestions
            .filter { it.timeOfDay == null || it.timeOfDay == timeOfDay }
            .sortedByDescending { it.timesAccepted }

    override suspend fun getByCategoryAndTimeOfDay(
        category: SuggestionCategory,
        timeOfDay: TimeOfDay,
    ): List<AnalogSuggestion> =
        suggestions
            .filter { it.category == category && (it.timeOfDay == null || it.timeOfDay == timeOfDay) }
            .sortedByDescending { it.timesAccepted }

    override suspend fun getCustom(): List<AnalogSuggestion> =
        suggestions.filter { it.isCustom }

    override suspend fun getTopAccepted(limit: Long): List<AnalogSuggestion> =
        suggestions.sortedByDescending { it.timesAccepted }.take(limit.toInt())

    override suspend fun insert(suggestion: AnalogSuggestion): Long {
        val id = nextId++
        suggestions += suggestion.copy(id = id)
        return id
    }

    override suspend fun update(suggestion: AnalogSuggestion) {
        val idx = suggestions.indexOfFirst { it.id == suggestion.id }
        if (idx >= 0) suggestions[idx] = suggestion
    }

    override suspend fun recordShown(id: Long) {
        val idx = suggestions.indexOfFirst { it.id == id }
        if (idx >= 0) {
            suggestions[idx] = suggestions[idx].let { it.copy(timesShown = it.timesShown + 1) }
        }
    }

    override suspend fun recordAccepted(id: Long) {
        val idx = suggestions.indexOfFirst { it.id == id }
        if (idx >= 0) {
            suggestions[idx] = suggestions[idx].let { it.copy(timesAccepted = it.timesAccepted + 1) }
        }
    }

    override suspend fun deleteById(id: Long) {
        suggestions.removeAll { it.id == id }
    }
}
