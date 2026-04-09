package dev.spark.domain

data class AnalogSuggestion(
    val id: Long = 0,
    val text: String,
    val category: SuggestionCategory,
    val tags: List<String>,
    val timeOfDay: TimeOfDay? = null,
    val timesShown: Int = 0,
    val timesAccepted: Int = 0,
    val isCustom: Boolean = false
)

enum class SuggestionCategory {
    EXERCISE, CREATIVE, SOCIAL, MINDFULNESS, LEARNING, NATURE, COOKING, MUSIC, GAMING_PHYSICAL, READING
}

enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT
}
