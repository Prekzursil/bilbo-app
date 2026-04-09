package dev.spark.data

import dev.spark.domain.AnalogSuggestion
import dev.spark.domain.AppCategory
import dev.spark.domain.AppProfile
import dev.spark.domain.EnforcementMode
import dev.spark.domain.SuggestionCategory
import dev.spark.domain.TimeOfDay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Preference key ─────────────────────────────────────────────────────────────

private const val KEY_SEEDED = "spark_seed_data_loaded_v1"

// ── JSON data-transfer objects ─────────────────────────────────────────────────

@Serializable
private data class AppClassificationDto(
    val packageName: String,
    val appLabel: String,
    val category: String,
    val defaultEnforcementMode: String,
)

@Serializable
private data class AnalogSuggestionDto(
    val id: Long = 0,
    val text: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val timeOfDay: String? = null,
)

// ── Lenient JSON parser (ignores unknown keys from future schema additions) ────

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Loads default seed data into the database on the first app launch.
 *
 * **Responsibilities:**
 * 1. Check [SeedPreferenceStore.isSeeded]; if `true`, return immediately.
 * 2. Read `default_app_classifications.json` via [ResourceReader] and upsert
 *    each entry into [AppProfileRepository].
 * 3. Read `default_analog_suggestions.json` and insert each entry into
 *    [SuggestionRepository] (skips duplicates via id uniqueness constraint).
 * 4. Set [SeedPreferenceStore.isSeeded] = `true`.
 *
 * Call [load] once at startup (e.g. from `SparkApplication.onCreate` or a
 * dedicated Hilt-injected `AppInitializer`).
 *
 * @param appProfileRepository Target repository for app classifications.
 * @param suggestionRepository Target repository for analog suggestions.
 * @param resourceReader       Platform-specific raw-resource reader.
 * @param prefStore            Lightweight flag store that tracks seeding status.
 */
class SeedDataLoader(
    private val appProfileRepository: AppProfileRepository,
    private val suggestionRepository: SuggestionRepository,
    private val resourceReader: ResourceReader,
    private val prefStore: SeedPreferenceStore,
) {

    /**
     * Loads seed data if it has not already been loaded.
     * Safe to call on every app start — idempotent after first run.
     *
     * @return `true` if seeding was performed, `false` if already seeded.
     */
    suspend fun load(): Boolean {
        if (prefStore.isSeeded()) return false

        loadAppClassifications()
        loadAnalogSuggestions()

        prefStore.markSeeded()
        return true
    }

    /**
     * Forces re-seeding regardless of the preference flag.
     * Useful for development or data migrations.
     */
    suspend fun forceReload() {
        prefStore.clearSeeded()
        load()
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private suspend fun loadAppClassifications() {
        val raw = resourceReader.readText("default_app_classifications.json")
        val dtos = json.decodeFromString<List<AppClassificationDto>>(raw)

        dtos.forEach { dto ->
            val profile = AppProfile(
                packageName             = dto.packageName,
                appLabel                = dto.appLabel,
                category                = dto.category.toAppCategory(),
                enforcementMode         = dto.defaultEnforcementMode.toEnforcementMode(),
                coolingOffEnabled       = false,
                isBypassed              = false,
                isCustomClassification  = false,
            )
            // Upsert: skip if already stored by a prior seeding or user modification
            val existing = appProfileRepository.getByPackageName(dto.packageName)
            if (existing == null) {
                appProfileRepository.insert(profile)
            }
        }
    }

    private suspend fun loadAnalogSuggestions() {
        val raw = resourceReader.readText("default_analog_suggestions.json")
        val dtos = json.decodeFromString<List<AnalogSuggestionDto>>(raw)

        // Only insert if table is empty (avoid duplicates on force-reload)
        val existingCount = suggestionRepository.getAll().count { !it.isCustom }
        if (existingCount > 0) return

        dtos.forEach { dto ->
            val suggestion = AnalogSuggestion(
                id         = 0,     // Let the DB assign the id
                text       = dto.text,
                category   = dto.category.toSuggestionCategory(),
                tags       = dto.tags,
                timeOfDay  = dto.timeOfDay?.toTimeOfDay(),
                isCustom   = false,
            )
            suggestionRepository.insert(suggestion)
        }
    }

    // ── String → enum conversions ──────────────────────────────────────────────

    private fun String.toAppCategory(): AppCategory = when (uppercase()) {
        "NUTRITIVE"      -> AppCategory.NUTRITIVE
        "NEUTRAL"        -> AppCategory.NEUTRAL
        "EMPTY_CALORIES" -> AppCategory.EMPTY_CALORIES
        else             -> AppCategory.NEUTRAL   // safe default
    }

    private fun String.toEnforcementMode(): EnforcementMode = when (uppercase()) {
        "HARD_LOCK" -> EnforcementMode.HARD_LOCK
        else        -> EnforcementMode.NUDGE
    }

    private fun String.toSuggestionCategory(): SuggestionCategory = when (uppercase()) {
        "EXERCISE"         -> SuggestionCategory.EXERCISE
        "CREATIVE"         -> SuggestionCategory.CREATIVE
        "SOCIAL"           -> SuggestionCategory.SOCIAL
        "MINDFULNESS"      -> SuggestionCategory.MINDFULNESS
        "LEARNING"         -> SuggestionCategory.LEARNING
        "NATURE"           -> SuggestionCategory.NATURE
        "COOKING"          -> SuggestionCategory.COOKING
        "MUSIC"            -> SuggestionCategory.MUSIC
        "GAMING_PHYSICAL"  -> SuggestionCategory.GAMING_PHYSICAL
        "READING"          -> SuggestionCategory.READING
        else               -> SuggestionCategory.READING  // safe default
    }

    private fun String.toTimeOfDay(): TimeOfDay = when (uppercase()) {
        "MORNING"   -> TimeOfDay.MORNING
        "AFTERNOON" -> TimeOfDay.AFTERNOON
        "EVENING"   -> TimeOfDay.EVENING
        "NIGHT"     -> TimeOfDay.NIGHT
        else        -> TimeOfDay.MORNING
    }
}

// ── Platform abstractions ──────────────────────────────────────────────────────

/**
 * Platform-specific reader for bundled resource files.
 *
 * Android implementation reads from `assets/` via [AssetManager].
 * iOS implementation reads from the app bundle using `Bundle.main.url(forResource:)`.
 */
interface ResourceReader {
    /**
     * Reads the full text content of a bundled resource file.
     *
     * @param fileName File name relative to the commonMain/resources directory.
     * @throws IllegalArgumentException if the resource cannot be found.
     */
    suspend fun readText(fileName: String): String
}

/**
 * Lightweight, platform-agnostic store for the "seed data loaded" flag.
 *
 * Android: backed by [SharedPreferences].
 * iOS: backed by `UserDefaults`.
 */
interface SeedPreferenceStore {
    /** Returns `true` if seed data has been successfully loaded. */
    fun isSeeded(): Boolean

    /** Marks seed data as loaded. */
    fun markSeeded()

    /** Clears the seeded flag (for re-seeding / development). */
    fun clearSeeded()
}
