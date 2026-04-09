package dev.bilbo.economy

import dev.bilbo.domain.*

/**
 * Manages app categorization.
 *
 * Built-in defaults come from the bundled JSON seed data (loaded at startup).
 * Users may override any classification, and overrides take precedence.
 */
class AppClassifier(
    private val builtInDefaults: Map<String, AppClassification>,
    private val userOverrides: MutableMap<String, AppClassification> = mutableMapOf()
) {

    data class AppClassification(
        val packageName: String,
        val appLabel: String,
        val category: AppCategory,
        val defaultEnforcementMode: EnforcementMode
    )

    // Package-name prefix heuristics for unknown apps
    private val knownSocialPrefixes = listOf(
        "com.facebook", "com.instagram", "com.twitter", "com.snapchat",
        "com.tiktok", "com.reddit", "com.tumblr", "com.pinterest"
    )
    private val knownProductivityPrefixes = listOf(
        "com.google.android.apps.docs", "com.microsoft", "com.slack",
        "com.notion", "com.todoist", "com.evernote"
    )
    private val knownLearningPrefixes = listOf(
        "com.duolingo", "com.coursera", "org.khanacademy",
        "com.audible", "com.amazon.kindle"
    )

    // -------------------------------------------------------------------------
    // Classification lookup
    // -------------------------------------------------------------------------

    /**
     * Returns the effective [AppClassification] for [packageName].
     * Priority: user override → built-in default → heuristic guess → null.
     */
    fun classify(packageName: String): AppClassification? {
        return userOverrides[packageName]
            ?: builtInDefaults[packageName]
            ?: inferFromPackageName(packageName)
    }

    /**
     * Returns the [AppProfile] for [packageName], or null if unclassified.
     */
    fun getProfile(packageName: String, appLabel: String): AppProfile? {
        val classification = classify(packageName) ?: return null
        return AppProfile(
            packageName = packageName,
            appLabel = appLabel,
            category = classification.category,
            enforcementMode = classification.defaultEnforcementMode,
            isCustomClassification = userOverrides.containsKey(packageName)
        )
    }

    // -------------------------------------------------------------------------
    // User overrides
    // -------------------------------------------------------------------------

    /**
     * Adds or updates a user override for [packageName].
     */
    fun setUserOverride(
        packageName: String,
        appLabel: String,
        category: AppCategory,
        enforcementMode: EnforcementMode
    ) {
        userOverrides[packageName] = AppClassification(
            packageName = packageName,
            appLabel = appLabel,
            category = category,
            defaultEnforcementMode = enforcementMode
        )
    }

    /**
     * Removes a user override, reverting the app to its built-in classification.
     */
    fun removeUserOverride(packageName: String) {
        userOverrides.remove(packageName)
    }

    /**
     * Returns true if the user has a custom override for [packageName].
     */
    fun hasUserOverride(packageName: String): Boolean = userOverrides.containsKey(packageName)

    /**
     * Returns all current user overrides.
     */
    fun getUserOverrides(): Map<String, AppClassification> = userOverrides.toMap()

    // -------------------------------------------------------------------------
    // Bulk operations
    // -------------------------------------------------------------------------

    /**
     * Returns all known classifications (built-in + user overrides merged).
     */
    fun getAllClassifications(): Map<String, AppClassification> =
        builtInDefaults + userOverrides

    /**
     * Returns all apps in a given [category].
     */
    fun getAppsInCategory(category: AppCategory): List<AppClassification> =
        getAllClassifications().values.filter { it.category == category }

    /**
     * Returns a list of package names with no classification (requires user input).
     */
    fun getUnclassified(installedPackages: List<String>): List<String> =
        installedPackages.filter { classify(it) == null }

    // -------------------------------------------------------------------------
    // Factory / seed loading
    // -------------------------------------------------------------------------

    companion object {
        fun fromDefaults(defaults: List<AppClassification>): AppClassifier {
            val map = defaults.associateBy { it.packageName }
            return AppClassifier(builtInDefaults = map)
        }

        fun fromDefaults(
            defaults: List<AppClassification>,
            overrides: List<AppClassification>
        ): AppClassifier {
            val defaultMap = defaults.associateBy { it.packageName }
            val overrideMap = overrides.associateBy { it.packageName }.toMutableMap()
            return AppClassifier(builtInDefaults = defaultMap, userOverrides = overrideMap)
        }
    }

    // -------------------------------------------------------------------------
    // Heuristic inference for unknown apps
    // -------------------------------------------------------------------------

    private fun inferFromPackageName(packageName: String): AppClassification? {
        val lower = packageName.lowercase()

        val category = when {
            knownSocialPrefixes.any { lower.startsWith(it) } -> AppCategory.EMPTY_CALORIES
            knownLearningPrefixes.any { lower.startsWith(it) } -> AppCategory.NUTRITIVE
            knownProductivityPrefixes.any { lower.startsWith(it) } -> AppCategory.NEUTRAL
            else -> return null
        }

        val enforcement = if (category == AppCategory.EMPTY_CALORIES)
            EnforcementMode.NUDGE else EnforcementMode.NUDGE

        return AppClassification(
            packageName = packageName,
            appLabel = packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
            category = category,
            defaultEnforcementMode = enforcement
        )
    }
}
