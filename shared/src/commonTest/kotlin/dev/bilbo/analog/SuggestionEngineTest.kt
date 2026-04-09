package dev.bilbo.analog

import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

// =============================================================================
//  Fake repositories
// =============================================================================

private class FakeSuggestionRepo : SuggestionRepository {
    val suggestions = mutableListOf<AnalogSuggestion>()
    var shownIds = mutableListOf<Long>()
    var acceptedIds = mutableListOf<Long>()
    var deletedIds = mutableListOf<Long>()
    private var nextId = 100L

    override fun observeAll(): Flow<List<AnalogSuggestion>> = flowOf(suggestions)
    override suspend fun getAll(): List<AnalogSuggestion> = suggestions.toList()
    override suspend fun getById(id: Long): AnalogSuggestion? = suggestions.find { it.id == id }
    override suspend fun getByCategory(category: SuggestionCategory): List<AnalogSuggestion> =
        suggestions.filter { it.category == category }
    override suspend fun getByTimeOfDay(timeOfDay: TimeOfDay): List<AnalogSuggestion> =
        suggestions.filter { it.timeOfDay == null || it.timeOfDay == timeOfDay }
    override suspend fun getByCategoryAndTimeOfDay(category: SuggestionCategory, timeOfDay: TimeOfDay): List<AnalogSuggestion> =
        suggestions.filter { it.category == category && (it.timeOfDay == null || it.timeOfDay == timeOfDay) }
    override suspend fun getCustom(): List<AnalogSuggestion> = suggestions.filter { it.isCustom }
    override suspend fun getTopAccepted(limit: Long): List<AnalogSuggestion> =
        suggestions.sortedByDescending { it.timesAccepted }.take(limit.toInt())
    override suspend fun insert(suggestion: AnalogSuggestion): Long {
        val id = nextId++
        suggestions.add(suggestion.copy(id = id))
        return id
    }
    override suspend fun update(suggestion: AnalogSuggestion) {
        val idx = suggestions.indexOfFirst { it.id == suggestion.id }
        if (idx >= 0) suggestions[idx] = suggestion
    }
    override suspend fun recordShown(id: Long) { shownIds.add(id) }
    override suspend fun recordAccepted(id: Long) { acceptedIds.add(id) }
    override suspend fun deleteById(id: Long) {
        deletedIds.add(id)
        suggestions.removeAll { it.id == id }
    }
}

private class FakeBudgetRepo : BudgetRepository {
    var bonusIncrements = mutableListOf<Pair<LocalDate, Int>>()

    override fun observeAll(): Flow<List<DopamineBudget>> = flowOf(emptyList())
    override suspend fun getAll(): List<DopamineBudget> = emptyList()
    override suspend fun getByDate(date: LocalDate): DopamineBudget? = null
    override fun observeByDate(date: LocalDate): Flow<DopamineBudget?> = flowOf(null)
    override suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<DopamineBudget> = emptyList()
    override suspend fun getRecent(limit: Long): List<DopamineBudget> = emptyList()
    override suspend fun insert(budget: DopamineBudget) {}
    override suspend fun update(budget: DopamineBudget) {}
    override suspend fun upsert(budget: DopamineBudget) {}
    override suspend fun incrementFpEarned(date: LocalDate, amount: Int) {}
    override suspend fun incrementFpSpent(date: LocalDate, amount: Int) {}
    override suspend fun incrementFpBonus(date: LocalDate, amount: Int) {
        bonusIncrements.add(date to amount)
    }
    override suspend fun deleteByDate(date: LocalDate) {}
    override suspend fun sumFpEarned(from: LocalDate, to: LocalDate): Long = 0
}

// =============================================================================
//  SuggestionEngine Tests
// =============================================================================

class SuggestionEngineGetSuggestionsTest {
    private lateinit var repo: FakeSuggestionRepo
    private lateinit var budgetRepo: FakeBudgetRepo

    @BeforeTest
    fun setup() {
        repo = FakeSuggestionRepo()
        budgetRepo = FakeBudgetRepo()
    }

    @Test fun getSuggestionsEmpty() = runTest {
        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE) }, { TimeOfDay.MORNING })
        val result = engine.getSuggestions()
        assertTrue(result.isEmpty())
    }

    @Test fun getSuggestionsFiltersbyTime() = runTest {
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Morning Walk", category = SuggestionCategory.EXERCISE, tags = emptyList(), timeOfDay = TimeOfDay.MORNING))
        repo.suggestions.add(AnalogSuggestion(id = 2, text = "Night Read", category = SuggestionCategory.READING, tags = emptyList(), timeOfDay = TimeOfDay.NIGHT))

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE, SuggestionCategory.READING) }, { TimeOfDay.MORNING })
        val result = engine.getSuggestions()
        assertEquals(1, result.size)
        assertEquals("Morning Walk", result[0].text)
    }

    @Test fun getSuggestionsIncludesNullTimeOfDay() = runTest {
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Anytime", category = SuggestionCategory.EXERCISE, tags = emptyList(), timeOfDay = null))

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE) }, { TimeOfDay.EVENING })
        val result = engine.getSuggestions()
        assertEquals(1, result.size)
    }

    @Test fun getSuggestionsFallsBackIfFewInterests() = runTest {
        // Add 3 exercise suggestions but user interested only in CREATIVE
        repeat(3) { i ->
            repo.suggestions.add(AnalogSuggestion(id = i.toLong() + 1, text = "Ex $i", category = SuggestionCategory.EXERCISE, tags = emptyList()))
        }

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.CREATIVE) }, { TimeOfDay.MORNING })
        // interest filter yields 0, which < count(3), falls back to time-filtered
        val result = engine.getSuggestions(3)
        assertEquals(3, result.size)
    }

    @Test fun getSuggestionsSortsByTimesShownThenAcceptanceRate() = runTest {
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "A", category = SuggestionCategory.EXERCISE, tags = emptyList(), timesShown = 10, timesAccepted = 5))
        repo.suggestions.add(AnalogSuggestion(id = 2, text = "B", category = SuggestionCategory.EXERCISE, tags = emptyList(), timesShown = 1, timesAccepted = 0))

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE) }, { TimeOfDay.MORNING })
        val result = engine.getSuggestions(2)
        assertEquals("B", result[0].text) // least shown first
        assertEquals("A", result[1].text)
    }

    @Test fun getSuggestionsTakesTopN() = runTest {
        repeat(10) { i ->
            repo.suggestions.add(AnalogSuggestion(id = i.toLong() + 1, text = "S$i", category = SuggestionCategory.EXERCISE, tags = emptyList()))
        }

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE) }, { TimeOfDay.MORNING })
        val result = engine.getSuggestions(3)
        assertEquals(3, result.size)
    }
}

class SuggestionEngineAcceptTest {
    @Test fun acceptSuggestionRecordsAndAwardsFP() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Walk", category = SuggestionCategory.EXERCISE, tags = emptyList()))

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        engine.acceptSuggestion(1)
        assertTrue(repo.acceptedIds.contains(1))
        assertEquals(1, budgetRepo.bonusIncrements.size)
        assertEquals(FPEconomy.BONUS_ANALOG_ACCEPTED, budgetRepo.bonusIncrements[0].second)
    }

    @Test fun showAnotherRecordsShown() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Walk", category = SuggestionCategory.EXERCISE, tags = emptyList()))

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        engine.showAnother(1)
        assertTrue(repo.shownIds.contains(1))
    }
}

class SuggestionEngineCustomTest {
    @Test fun addCustomSuggestion() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        val suggestion = AnalogSuggestion(text = "My idea", category = SuggestionCategory.CREATIVE, tags = listOf("fun"))
        val id = engine.addCustomSuggestion(suggestion)
        assertTrue(id > 0)
        assertTrue(repo.suggestions.last().isCustom)
    }

    @Test fun addCustomSuggestionBlankTextFails() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        val suggestion = AnalogSuggestion(text = "  ", category = SuggestionCategory.CREATIVE, tags = emptyList())
        assertFailsWith<IllegalArgumentException> {
            engine.addCustomSuggestion(suggestion)
        }
    }

    @Test fun deleteCustomSuggestion() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Custom", category = SuggestionCategory.CREATIVE, tags = emptyList(), isCustom = true))

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        engine.deleteCustomSuggestion(1)
        assertTrue(repo.deletedIds.contains(1))
    }

    @Test fun deleteNonCustomFails() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "Built-in", category = SuggestionCategory.EXERCISE, tags = emptyList(), isCustom = false))

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        assertFailsWith<IllegalStateException> {
            engine.deleteCustomSuggestion(1)
        }
    }

    @Test fun deleteNonExistentFails() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()

        val engine = SuggestionEngine(repo, budgetRepo, { emptySet() }, { TimeOfDay.MORNING })
        assertFailsWith<IllegalStateException> {
            engine.deleteCustomSuggestion(999)
        }
    }
}

class SuggestionEngineResolveTimeOfDayTest {
    // Test helper: create a fake clock at a specific hour (UTC)
    private fun clockAt(hour: Int): Clock = object : Clock {
        override fun now(): Instant =
            LocalDateTime(2025, 6, 15, hour, 30).toInstant(TimeZone.currentSystemDefault())
    }

    @Test fun resolveTimeOfDayDefaultClock() {
        // Call without clock parameter to cover the $default bridge
        val tod = SuggestionEngine.resolveTimeOfDay()
        assertNotNull(tod)
    }

    @Test fun resolveMorning5am() {
        assertEquals(TimeOfDay.MORNING, SuggestionEngine.resolveTimeOfDay(clockAt(5)))
    }

    @Test fun resolveMorning11am() {
        assertEquals(TimeOfDay.MORNING, SuggestionEngine.resolveTimeOfDay(clockAt(11)))
    }

    @Test fun resolveAfternoon12pm() {
        assertEquals(TimeOfDay.AFTERNOON, SuggestionEngine.resolveTimeOfDay(clockAt(12)))
    }

    @Test fun resolveAfternoon16pm() {
        assertEquals(TimeOfDay.AFTERNOON, SuggestionEngine.resolveTimeOfDay(clockAt(16)))
    }

    @Test fun resolveEvening17pm() {
        assertEquals(TimeOfDay.EVENING, SuggestionEngine.resolveTimeOfDay(clockAt(17)))
    }

    @Test fun resolveEvening20pm() {
        assertEquals(TimeOfDay.EVENING, SuggestionEngine.resolveTimeOfDay(clockAt(20)))
    }

    @Test fun resolveNight21pm() {
        assertEquals(TimeOfDay.NIGHT, SuggestionEngine.resolveTimeOfDay(clockAt(21)))
    }

    @Test fun resolveNight0am() {
        assertEquals(TimeOfDay.NIGHT, SuggestionEngine.resolveTimeOfDay(clockAt(0)))
    }

    @Test fun resolveNight4am() {
        assertEquals(TimeOfDay.NIGHT, SuggestionEngine.resolveTimeOfDay(clockAt(4)))
    }
}

class SuggestionEngineAcceptanceRateTest {
    @Test fun acceptanceRateZeroShown() = runTest {
        val repo = FakeSuggestionRepo()
        val budgetRepo = FakeBudgetRepo()
        // timesShown=0, timesAccepted=0 -> acceptanceRate = 0
        repo.suggestions.add(AnalogSuggestion(id = 1, text = "A", category = SuggestionCategory.EXERCISE, tags = emptyList(), timesShown = 0, timesAccepted = 0))
        // timesShown=10, timesAccepted=5 -> acceptanceRate = 0.5
        repo.suggestions.add(AnalogSuggestion(id = 2, text = "B", category = SuggestionCategory.EXERCISE, tags = emptyList(), timesShown = 10, timesAccepted = 5))

        val engine = SuggestionEngine(repo, budgetRepo, { setOf(SuggestionCategory.EXERCISE) }, { TimeOfDay.MORNING })
        val result = engine.getSuggestions(2)
        // Both have timesShown so A (0) is shown first, then B (10)
        assertEquals("A", result[0].text)
        assertEquals("B", result[1].text)
    }
}
