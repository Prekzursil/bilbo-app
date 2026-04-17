package dev.bilbo.app.ui.dashboard

import app.cash.turbine.test
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for [DashboardViewModel].
 *
 * Covers:
 *  - today-only filtering (sessions before local-midnight are excluded)
 *  - per-package aggregation and ordering (descending by minutes)
 *  - category resolution falling back to the session value when no profile exists
 *  - category override via [AppProfileRepository] when a profile *is* present
 *  - goal-delta copy for both under- and over-goal totals
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun session(
        id: Long,
        pkg: String,
        label: String,
        minutes: Int,
        start: Instant,
        category: AppCategory = AppCategory.NEUTRAL,
    ) = UsageSession(
        id = id,
        packageName = pkg,
        appLabel = label,
        category = category,
        startTime = start,
        endTime = null,
        durationSeconds = minutes * 60L,
        wasTracked = true,
    )

    private class FakeUsageRepository(initial: List<UsageSession> = emptyList()) : UsageRepository {
        private val state = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<UsageSession>> = state.asStateFlow()
        override suspend fun getAll(): List<UsageSession> = state.value
        override suspend fun getById(id: Long): UsageSession? = state.value.firstOrNull { it.id == id }
        override suspend fun getByPackageName(packageName: String): List<UsageSession> =
            state.value.filter { it.packageName == packageName }
        override suspend fun getByDateRange(from: Instant, to: Instant): List<UsageSession> =
            state.value.filter { it.startTime in from..to }
        override suspend fun getByCategory(category: AppCategory): List<UsageSession> =
            state.value.filter { it.category == category }
        override suspend fun getByDateRangeAndCategory(
            from: Instant, to: Instant, category: AppCategory,
        ): List<UsageSession> =
            state.value.filter { it.startTime in from..to && it.category == category }
        override suspend fun insert(session: UsageSession): Long {
            state.value = state.value + session
            return session.id
        }
        override suspend fun updateEndTime(id: Long, endTime: Instant, durationSeconds: Long) = Unit
        override suspend fun deleteById(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }
        override suspend fun deleteOlderThan(before: Instant) = Unit
        override suspend fun countByPackageName(packageName: String): Long =
            state.value.count { it.packageName == packageName }.toLong()
        override suspend fun sumDurationByCategory(from: Instant, to: Instant): Map<AppCategory, Long> =
            state.value.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.durationSeconds } }
    }

    private class FakeAppProfileRepository(initial: Map<String, AppProfile> = emptyMap()) :
        AppProfileRepository {
        private val state = MutableStateFlow(initial)
        override fun observeAll(): Flow<List<AppProfile>> =
            state.asStateFlow().map { it.values.toList() }
        override suspend fun getAll(): List<AppProfile> = state.value.values.toList()
        override suspend fun getByPackageName(packageName: String): AppProfile? =
            state.value[packageName]
        override fun observeByPackageName(packageName: String): Flow<AppProfile?> =
            state.asStateFlow().map { it[packageName] }
        override suspend fun getByCategory(category: AppCategory): List<AppProfile> =
            state.value.values.filter { it.category == category }
        override suspend fun getByEnforcementMode(
            enforcementMode: EnforcementMode,
        ): List<AppProfile> = state.value.values.filter { it.enforcementMode == enforcementMode }
        override suspend fun getBypassed(): List<AppProfile> =
            state.value.values.filter { it.isBypassed }
        override suspend fun getCustomClassified(): List<AppProfile> =
            state.value.values.filter { it.isCustomClassification }
        override suspend fun insert(profile: AppProfile) {
            state.value = state.value + (profile.packageName to profile)
        }
        override suspend fun update(profile: AppProfile) {
            state.value = state.value + (profile.packageName to profile)
        }
        override suspend fun upsert(profile: AppProfile) {
            state.value = state.value + (profile.packageName to profile)
        }
        override suspend fun updateCategory(packageName: String, category: AppCategory) = Unit
        override suspend fun updateBypass(packageName: String, isBypassed: Boolean) = Unit
        override suspend fun deleteByPackageName(packageName: String) {
            state.value = state.value - packageName
        }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `filters out sessions before local midnight`() = runTest(dispatcher) {
        val tz = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)
        val yesterday = startOfToday.minus(1.hours)
        val nowish = startOfToday.plus(1.hours)

        val repo = FakeUsageRepository(
            listOf(
                session(1, "com.a", "App A", minutes = 30, start = yesterday),
                session(2, "com.b", "App B", minutes = 10, start = nowish),
            ),
        )
        val vm = DashboardViewModel(repo, FakeAppProfileRepository())

        advanceUntilIdle()
        vm.uiState.test {
            // Drop initial loading emission if present, read settled state.
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(1, state.apps.size, "Only today's session should survive the filter")
            assertEquals("com.b", state.apps[0].packageName)
            assertEquals(10, state.totalMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `aggregates per package and sorts descending`() = runTest(dispatcher) {
        val tz = TimeZone.currentSystemDefault()
        val start = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)
            .plus(1.hours)

        val repo = FakeUsageRepository(
            listOf(
                session(1, "com.a", "App A", minutes = 15, start = start),
                session(2, "com.a", "App A", minutes = 25, start = start),  // total 40m
                session(3, "com.b", "App B", minutes = 60, start = start),  // total 60m
            ),
        )
        val vm = DashboardViewModel(repo, FakeAppProfileRepository())

        advanceUntilIdle()
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(2, state.apps.size)
            assertEquals("com.b", state.apps[0].packageName)  // sorted desc
            assertEquals(60, state.apps[0].durationMinutes)
            assertEquals("com.a", state.apps[1].packageName)
            assertEquals(40, state.apps[1].durationMinutes)
            assertEquals(100, state.totalMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uses app profile category when present`() = runTest(dispatcher) {
        val tz = TimeZone.currentSystemDefault()
        val start = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)
            .plus(2.hours)

        val repo = FakeUsageRepository(
            listOf(
                session(1, "com.social", "Social App", minutes = 20, start = start,
                    category = AppCategory.NEUTRAL),
            ),
        )
        val profiles = FakeAppProfileRepository(
            mapOf(
                "com.social" to AppProfile(
                    packageName = "com.social",
                    appLabel = "Custom Label",
                    category = AppCategory.EMPTY_CALORIES,
                    enforcementMode = EnforcementMode.NUDGE,
                ),
            ),
        )
        val vm = DashboardViewModel(repo, profiles)

        advanceUntilIdle()
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertEquals(1, state.apps.size)
            assertEquals(AppCategory.EMPTY_CALORIES, state.apps[0].category)
            assertEquals("Custom Label", state.apps[0].appLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `goal delta copy reports under goal`() {
        val state = DashboardViewModel.DashboardUiState(
            isLoading = false,
            totalMinutes = 90,
            dailyGoalMinutes = 150,
        )
        assertEquals("60 min under your daily goal", state.goalDeltaCopy)
        assertTrue("1h 30m" == state.formattedTotal)
    }

    @Test
    fun `goal delta copy reports over goal`() {
        val state = DashboardViewModel.DashboardUiState(
            isLoading = false,
            totalMinutes = 200,
            dailyGoalMinutes = 150,
        )
        assertEquals("50 min over your daily goal", state.goalDeltaCopy)
        assertEquals("3h 20m", state.formattedTotal)
    }
}
