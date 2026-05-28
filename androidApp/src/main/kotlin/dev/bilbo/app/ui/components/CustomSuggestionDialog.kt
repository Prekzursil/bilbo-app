package dev.bilbo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay

private const val ARGB_CHIP_SELECTED = 0xFF40916C
private const val ARGB_CHIP_SELECTED_ON = 0xFFD8F3DC

private const val MIN_TEXT_LENGTH = 5
private const val MAX_TEXT_LENGTH = 200
private const val TEXT_MAX_LINES = 3
private const val DIALOG_RADIUS_DP = 20
private const val FIELD_RADIUS_DP = 12
private const val SECTION_SPACE_DP = 16
private const val CHIP_SPACE_DP = 8
private const val LABEL_SPACE_DP = 4

// ── Chip accent ────────────────────────────────────────────────────────────────
private val ChipSelected = Color(ARGB_CHIP_SELECTED)
private val ChipSelectedOn = Color(ARGB_CHIP_SELECTED_ON)

// ── Metadata maps ──────────────────────────────────────────────────────────────

private val categoryEntries: List<Pair<SuggestionCategory, String>> =
    listOf(
        SuggestionCategory.READING to "📚 Reading",
        SuggestionCategory.EXERCISE to "💪 Exercise",
        SuggestionCategory.COOKING to "🍳 Cooking",
        SuggestionCategory.CREATIVE to "🎨 Creative",
        SuggestionCategory.MUSIC to "🎵 Music",
        SuggestionCategory.NATURE to "🌿 Nature",
        SuggestionCategory.SOCIAL to "👥 Social",
        SuggestionCategory.MINDFULNESS to "🧘 Mindfulness",
        SuggestionCategory.GAMING_PHYSICAL to "🎲 Physical Games",
        SuggestionCategory.LEARNING to "📖 Learning",
    )

private val timeOfDayEntries: List<Pair<TimeOfDay?, String>> =
    listOf(
        null to "⏰ Any time",
        TimeOfDay.MORNING to "🌅 Morning",
        TimeOfDay.AFTERNOON to "☀️ Afternoon",
        TimeOfDay.EVENING to "🌙 Evening",
        TimeOfDay.NIGHT to "🌃 Night",
    )

/**
 * Dialog for creating a custom analog suggestion.
 *
 * Fields:
 * - Free-text suggestion input (min 5 chars, max 200 chars).
 * - [SuggestionCategory] single-select chips.
 * - [TimeOfDay] single-select chips (nullable — "Any time" maps to null).
 *
 * @param onSave    Invoked with a fully-populated [AnalogSuggestion] (id = 0, isCustom = true).
 * @param onDismiss Invoked when the user cancels.
 * @param initialText Optional pre-fill for the text field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomSuggestionDialog(
    onSave: (AnalogSuggestion) -> Unit,
    onDismiss: () -> Unit,
    initialText: String = "",
) {
    // ── Local state ────────────────────────────────────────────────────────
    var text by remember { mutableStateOf(initialText) }
    var category by remember { mutableStateOf<SuggestionCategory?>(null) }
    var timeOfDay by remember { mutableStateOf<TimeOfDay?>(null) }
    var textError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        textError = validateText(text)
        categoryError = if (category == null) "Pick a category." else null
        if (textError == null && categoryError == null) {
            onSave(
                AnalogSuggestion(
                    id = 0,
                    text = text.trim(),
                    category = category!!,
                    tags = emptyList(),
                    timeOfDay = timeOfDay,
                    isCustom = true,
                ),
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DIALOG_RADIUS_DP.dp),
        title = {
            Text(
                text = "Add Your Own",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SECTION_SPACE_DP.dp)) {
                SuggestionTextField(
                    text = text,
                    textError = textError,
                    onTextChange = {
                        text = it.take(MAX_TEXT_LENGTH)
                        textError = null
                    },
                )
                CategoryChips(
                    selected = category,
                    error = categoryError,
                    onSelect = {
                        category = it
                        categoryError = null
                    },
                )
                TimeOfDayChips(selected = timeOfDay, onSelect = { timeOfDay = it })
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun validateText(text: String): String? =
    when {
        text.isBlank() -> "Please enter a suggestion."
        text.length < MIN_TEXT_LENGTH -> "Too short — be a bit more descriptive."
        text.length > MAX_TEXT_LENGTH -> "Keep it under 200 characters."
        else -> null
    }

@Composable
private fun SuggestionTextField(
    text: String,
    textError: String?,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text("What will you do?") },
        placeholder = { Text("e.g. Make a cup of tea and read a chapter") },
        isError = textError != null,
        supportingText =
            textError?.let { err ->
                { Text(err, color = MaterialTheme.colorScheme.error) }
            },
        singleLine = false,
        maxLines = TEXT_MAX_LINES,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FIELD_RADIUS_DP.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    selected: SuggestionCategory?,
    error: String?,
    onSelect: (SuggestionCategory?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LABEL_SPACE_DP.dp)) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACE_DP.dp),
            verticalArrangement = Arrangement.spacedBy(LABEL_SPACE_DP.dp),
        ) {
            categoryEntries.forEach { (cat, label) ->
                val isSelected = selected == cat
                SelectableChip(
                    selected = isSelected,
                    label = label,
                    onClick = { onSelect(if (isSelected) null else cat) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeOfDayChips(
    selected: TimeOfDay?,
    onSelect: (TimeOfDay?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LABEL_SPACE_DP.dp)) {
        Text(
            text = "Time of day (optional)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACE_DP.dp),
            verticalArrangement = Arrangement.spacedBy(LABEL_SPACE_DP.dp),
        ) {
            timeOfDayEntries.forEach { (tod, label) ->
                val isSelected = selected == tod
                SelectableChip(
                    selected = isSelected,
                    label = label,
                    onClick = { onSelect(if (isSelected) null else tod) },
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = ChipSelected,
                selectedLabelColor = ChipSelectedOn,
            ),
    )
}
