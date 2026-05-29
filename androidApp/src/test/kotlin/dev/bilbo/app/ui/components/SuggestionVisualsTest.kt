package dev.bilbo.app.ui.components

import dev.bilbo.app.ui.components.SuggestionVisuals.emoji
import dev.bilbo.app.ui.components.SuggestionVisuals.label
import dev.bilbo.domain.SuggestionCategory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-JVM unit tests for [SuggestionVisuals]. Confirms every
 * [SuggestionCategory] enum value has a non-empty emoji and label, and that
 * the expected labels are present for spot-checked categories.
 */
class SuggestionVisualsTest {
    @Test
    fun `every category has a non-blank emoji`() {
        for (cat in SuggestionCategory.entries) {
            assertTrue(cat.emoji().isNotBlank(), "emoji for $cat")
        }
    }

    @Test
    fun `every category has a non-blank label`() {
        for (cat in SuggestionCategory.entries) {
            assertTrue(cat.label().isNotBlank(), "label for $cat")
        }
    }

    @Test
    fun `spot-check known emoji and label values`() {
        assertEquals("💪", SuggestionCategory.EXERCISE.emoji())
        assertEquals("Exercise", SuggestionCategory.EXERCISE.label())
        assertEquals("🎨", SuggestionCategory.CREATIVE.emoji())
        assertEquals("Creative", SuggestionCategory.CREATIVE.label())
        assertEquals("👥", SuggestionCategory.SOCIAL.emoji())
        assertEquals("Social", SuggestionCategory.SOCIAL.label())
        assertEquals("🧘", SuggestionCategory.MINDFULNESS.emoji())
        assertEquals("Mindfulness", SuggestionCategory.MINDFULNESS.label())
        assertEquals("📖", SuggestionCategory.LEARNING.emoji())
        assertEquals("Learning", SuggestionCategory.LEARNING.label())
        assertEquals("🌿", SuggestionCategory.NATURE.emoji())
        assertEquals("Nature", SuggestionCategory.NATURE.label())
        assertEquals("🍳", SuggestionCategory.COOKING.emoji())
        assertEquals("Cooking", SuggestionCategory.COOKING.label())
        assertEquals("🎵", SuggestionCategory.MUSIC.emoji())
        assertEquals("Music", SuggestionCategory.MUSIC.label())
        assertEquals("🎲", SuggestionCategory.GAMING_PHYSICAL.emoji())
        assertEquals("Physical Games", SuggestionCategory.GAMING_PHYSICAL.label())
        assertEquals("📚", SuggestionCategory.READING.emoji())
        assertEquals("Reading", SuggestionCategory.READING.label())
    }
}
