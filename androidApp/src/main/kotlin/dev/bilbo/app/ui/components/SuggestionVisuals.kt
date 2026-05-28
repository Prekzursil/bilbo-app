package dev.bilbo.app.ui.components

import dev.bilbo.domain.SuggestionCategory

/**
 * Shared emoji/label mappings for [SuggestionCategory].
 *
 * Kept as map lookups (rather than `when` expressions duplicated across screens)
 * so there is a single source of truth and no per-call cyclomatic complexity.
 */
object SuggestionVisuals {
    private val EMOJI: Map<SuggestionCategory, String> =
        mapOf(
            SuggestionCategory.EXERCISE to "💪",
            SuggestionCategory.CREATIVE to "🎨",
            SuggestionCategory.SOCIAL to "👥",
            SuggestionCategory.MINDFULNESS to "🧘",
            SuggestionCategory.LEARNING to "📖",
            SuggestionCategory.NATURE to "🌿",
            SuggestionCategory.COOKING to "🍳",
            SuggestionCategory.MUSIC to "🎵",
            SuggestionCategory.GAMING_PHYSICAL to "🎲",
            SuggestionCategory.READING to "📚",
        )

    private val LABEL: Map<SuggestionCategory, String> =
        mapOf(
            SuggestionCategory.EXERCISE to "Exercise",
            SuggestionCategory.CREATIVE to "Creative",
            SuggestionCategory.SOCIAL to "Social",
            SuggestionCategory.MINDFULNESS to "Mindfulness",
            SuggestionCategory.LEARNING to "Learning",
            SuggestionCategory.NATURE to "Nature",
            SuggestionCategory.COOKING to "Cooking",
            SuggestionCategory.MUSIC to "Music",
            SuggestionCategory.GAMING_PHYSICAL to "Physical Games",
            SuggestionCategory.READING to "Reading",
        )

    fun SuggestionCategory.emoji(): String = EMOJI.getValue(this)

    fun SuggestionCategory.label(): String = LABEL.getValue(this)
}
