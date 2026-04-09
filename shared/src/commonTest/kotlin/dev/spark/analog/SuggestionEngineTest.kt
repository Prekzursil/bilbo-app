// SuggestionEngineTest.kt
// Spark — Unit Tests: SuggestionEngine
//
// Tests: filtering (by category, interests, duration), sorting,
//        acceptance tracking, empty-state handling

package dev.spark.analog

import kotlin.test.*

// MARK: - Domain types

enum class SuggestionCategory {
    OUTDOORS, CREATIVE, SOCIAL, MINDFULNESS, LEARNING, PHYSICAL
}

data class AnalogSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val category: SuggestionCategory,
    val estimatedMinutes: Int,
    val interests: List<String> = emptyList()
)

data class SuggestionEngineResult(
    val suggestions: List<AnalogSuggestion>,
    val isEmpty: Boolean = suggestions.isEmpty()
)

// MARK: - SuggestionEngine under test

class SuggestionEngine(private val catalog: List<AnalogSuggestion>) {

    private val accepted = mutableSetOf<String>()   // IDs of accepted suggestions
    private val dismissed = mutableSetOf<String>()  // IDs of dismissed suggestions

    /**
     * Filter and sort suggestions.
     *
     * @param categories  Only include these categories (empty = all).
     * @param interests   Only include suggestions matching at least one interest (empty = all).
     * @param maxMinutes  Only include suggestions ≤ this duration (null = no limit).
     * @param excludeAccepted Exclude previously accepted suggestions.
     * @param excludeDismissed Exclude dismissed suggestions.
     */
    fun query(
        categories: List<SuggestionCategory> = emptyList(),
        interests: List<String> = emptyList(),
        maxMinutes: Int? = null,
        excludeAccepted: Boolean = true,
        excludeDismissed: Boolean = true
    ): SuggestionEngineResult {
        var filtered = catalog.asSequence()

        // Filter: accepted
        if (excludeAccepted) filtered = filtered.filter { it.id !in accepted }

        // Filter: dismissed
        if (excludeDismissed) filtered = filtered.filter { it.id !in dismissed }

        // Filter: category
        if (categories.isNotEmpty()) filtered = filtered.filter { it.category in categories }

        // Filter: interests
        if (interests.isNotEmpty()) filtered = filtered.filter { s ->
            s.interests.any { it in interests }
        }

        // Filter: duration
        if (maxMinutes != null) filtered = filtered.filter { it.estimatedMinutes <= maxMinutes }

        // Sort: shorter suggestions first, then alphabetically
        val sorted = filtered.sortedWith(
            compareBy({ it.estimatedMinutes }, { it.title })
        ).toList()

        return SuggestionEngineResult(sorted)
    }

    fun accept(id: String) { accepted.add(id) }
    fun dismiss(id: String) { dismissed.add(id) }
    fun clearHistory() { accepted.clear(); dismissed.clear() }

    fun isAccepted(id: String) = id in accepted
    fun isDismissed(id: String) = id in dismissed
}

// MARK: - Test catalog

private val testCatalog = listOf(
    AnalogSuggestion("1", "Go for a walk",       "Step outside for fresh air.",            SuggestionCategory.OUTDOORS,     20, listOf("nature", "fitness")),
    AnalogSuggestion("2", "Draw something",       "Sketch anything that comes to mind.",    SuggestionCategory.CREATIVE,     30, listOf("art", "creativity")),
    AnalogSuggestion("3", "Call a friend",        "Reconnect with someone you care about.", SuggestionCategory.SOCIAL,       15, listOf("social", "connection")),
    AnalogSuggestion("4", "Meditate",             "5 minutes of mindful breathing.",        SuggestionCategory.MINDFULNESS,   5, listOf("mindfulness", "calm")),
    AnalogSuggestion("5", "Read a book chapter",  "Pick up where you left off.",            SuggestionCategory.LEARNING,     30, listOf("reading", "learning")),
    AnalogSuggestion("6", "Do push-ups",          "A quick upper-body workout.",            SuggestionCategory.PHYSICAL,     10, listOf("fitness", "health")),
    AnalogSuggestion("7", "Water your plants",    "Check in on your green friends.",        SuggestionCategory.OUTDOORS,      5, listOf("nature", "home")),
    AnalogSuggestion("8", "Write in a journal",   "Capture your thoughts on paper.",        SuggestionCategory.CREATIVE,     15, listOf("writing", "creativity")),
    AnalogSuggestion("9", "Cook a new recipe",    "Try something from your saved list.",    SuggestionCategory.CREATIVE,     60, listOf("cooking", "creativity")),
    AnalogSuggestion("10","Stretch for 5 min",    "Relieve tension with gentle stretches.", SuggestionCategory.PHYSICAL,      5, listOf("fitness", "mindfulness"))
)

// MARK: - Tests

class SuggestionEngineTest {

    private lateinit var engine: SuggestionEngine

    @BeforeTest
    fun setUp() {
        engine = SuggestionEngine(testCatalog)
    }

    // ── No filters ────────────────────────────────────────────────────────

    @Test
    fun testNoFilterReturnsAllSuggestions() {
        val result = engine.query()
        assertEquals(testCatalog.size, result.suggestions.size)
    }

    @Test
    fun testIsEmptyFalseWhenResultsExist() {
        val result = engine.query()
        assertFalse(result.isEmpty)
    }

    // ── Category filter ───────────────────────────────────────────────────

    @Test
    fun testFilterBySingleCategory() {
        val result = engine.query(categories = listOf(SuggestionCategory.OUTDOORS))
        assertTrue(result.suggestions.all { it.category == SuggestionCategory.OUTDOORS })
    }

    @Test
    fun testFilterByMultipleCategories() {
        val result = engine.query(categories = listOf(SuggestionCategory.MINDFULNESS, SuggestionCategory.PHYSICAL))
        assertTrue(result.suggestions.all {
            it.category == SuggestionCategory.MINDFULNESS || it.category == SuggestionCategory.PHYSICAL
        })
    }

    @Test
    fun testFilterByNonExistentCategoryReturnsEmpty() {
        // SOCIAL category but all social suggestions excluded via acceptance
        engine.accept("3")
        val result = engine.query(categories = listOf(SuggestionCategory.SOCIAL))
        assertTrue(result.isEmpty)
    }

    @Test
    fun testOutdoorsCategoryReturnsCorrectCount() {
        val result = engine.query(categories = listOf(SuggestionCategory.OUTDOORS))
        assertEquals(2, result.suggestions.size) // "Go for a walk" + "Water your plants"
    }

    // ── Interest filter ───────────────────────────────────────────────────

    @Test
    fun testFilterByInterestMatchesSuggestions() {
        val result = engine.query(interests = listOf("fitness"))
        assertTrue(result.suggestions.all { s -> "fitness" in s.interests })
    }

    @Test
    fun testFilterByMultipleInterestsUnion() {
        // "art" matches draw; "reading" matches book chapter
        val result = engine.query(interests = listOf("art", "reading"))
        assertEquals(2, result.suggestions.size)
    }

    @Test
    fun testFilterByUnknownInterestReturnsEmpty() {
        val result = engine.query(interests = listOf("underwater_basket_weaving"))
        assertTrue(result.isEmpty)
    }

    // ── Duration filter ───────────────────────────────────────────────────

    @Test
    fun testFilterByMaxMinutesExcludesLonger() {
        val result = engine.query(maxMinutes = 10)
        assertTrue(result.suggestions.all { it.estimatedMinutes <= 10 })
    }

    @Test
    fun testFilterByMaxFiveMinutes() {
        val result = engine.query(maxMinutes = 5)
        assertEquals(3, result.suggestions.size) // meditate(5), water plants(5), stretch(5)
    }

    @Test
    fun testFilterByMaxOneMinuteReturnsEmpty() {
        val result = engine.query(maxMinutes = 1)
        assertTrue(result.isEmpty)
    }

    @Test
    fun testNullMaxMinutesDoesNotFilter() {
        val result = engine.query(maxMinutes = null)
        assertEquals(testCatalog.size, result.suggestions.size)
    }

    // ── Sorting ───────────────────────────────────────────────────────────

    @Test
    fun testResultsSortedByDurationAscending() {
        val result = engine.query()
        val durations = result.suggestions.map { it.estimatedMinutes }
        assertEquals(durations.sorted(), durations)
    }

    @Test
    fun testTieBrokenAlphabetically() {
        // Several 5-min suggestions: "Meditate", "Stretch for 5 min", "Water your plants"
        val fiveMin = engine.query(maxMinutes = 5).suggestions.filter { it.estimatedMinutes == 5 }
        assertEquals(fiveMin.sortedBy { it.title }, fiveMin)
    }

    // ── Acceptance ────────────────────────────────────────────────────────

    @Test
    fun testAcceptedSuggestionExcludedByDefault() {
        engine.accept("1") // Go for a walk
        val result = engine.query()
        assertFalse(result.suggestions.any { it.id == "1" })
    }

    @Test
    fun testAcceptedSuggestionIncludedWhenFlagOff() {
        engine.accept("1")
        val result = engine.query(excludeAccepted = false)
        assertTrue(result.suggestions.any { it.id == "1" })
    }

    @Test
    fun testIsAcceptedReturnsTrueAfterAccept() {
        engine.accept("1")
        assertTrue(engine.isAccepted("1"))
    }

    @Test
    fun testIsAcceptedReturnsFalseBeforeAccept() {
        assertFalse(engine.isAccepted("99"))
    }

    // ── Dismissal ─────────────────────────────────────────────────────────

    @Test
    fun testDismissedSuggestionExcludedByDefault() {
        engine.dismiss("4") // Meditate
        val result = engine.query()
        assertFalse(result.suggestions.any { it.id == "4" })
    }

    @Test
    fun testDismissedSuggestionIncludedWhenFlagOff() {
        engine.dismiss("4")
        val result = engine.query(excludeDismissed = false)
        assertTrue(result.suggestions.any { it.id == "4" })
    }

    @Test
    fun testIsDismissedReturnsTrueAfterDismiss() {
        engine.dismiss("4")
        assertTrue(engine.isDismissed("4"))
    }

    // ── Combined filters ──────────────────────────────────────────────────

    @Test
    fun testCategoryAndDurationFilter() {
        val result = engine.query(
            categories = listOf(SuggestionCategory.PHYSICAL),
            maxMinutes = 5
        )
        assertTrue(result.suggestions.all {
            it.category == SuggestionCategory.PHYSICAL && it.estimatedMinutes <= 5
        })
    }

    @Test
    fun testInterestAndDurationFilter() {
        val result = engine.query(interests = listOf("creativity"), maxMinutes = 20)
        assertTrue(result.suggestions.all {
            "creativity" in it.interests && it.estimatedMinutes <= 20
        })
    }

    // ── Clear history ─────────────────────────────────────────────────────

    @Test
    fun testClearHistoryRestoresAcceptedSuggestions() {
        engine.accept("1")
        engine.clearHistory()
        val result = engine.query()
        assertTrue(result.suggestions.any { it.id == "1" })
    }

    @Test
    fun testClearHistoryRestoresDismissedSuggestions() {
        engine.dismiss("4")
        engine.clearHistory()
        val result = engine.query()
        assertTrue(result.suggestions.any { it.id == "4" })
    }

    // ── Empty catalog ─────────────────────────────────────────────────────

    @Test
    fun testEmptyCatalogReturnsEmptyResult() {
        val emptyEngine = SuggestionEngine(emptyList())
        val result = emptyEngine.query()
        assertTrue(result.isEmpty)
        assertTrue(result.suggestions.isEmpty())
    }
}
