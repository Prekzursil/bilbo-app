package dev.bilbo.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay

// ── Chip accent ────────────────────────────────────────────────────────────────
private val ChipSelected   = Color(0xFF40916C)
private val ChipSelectedOn = Color(0xFFD8F3DC)

// ── Metadata maps ──────────────────────────────────────────────────────────────

private val categoryEntries: List<Pair<SuggestionCategory, String>> = listOf(
    SuggestionCategory.READING         to "📚 Reading",
    SuggestionCategory.EXERCISE        to "💪 Exercise",
    SuggestionCategory.COOKING         to "🍳 Cooking",
    SuggestionCategory.CREATIVE        to "🎨 Creative",
    SuggestionCategory.MUSIC           to "🎵 Music",
    SuggestionCategory.NATURE          to "🌿 Nature",
    SuggestionCategory.SOCIAL          to "👥 Social",
    SuggestionCategory.MINDFULNESS     to "🧘 Mindfulness",
    SuggestionCategory.GAMING_PHYSICAL to "🎲 Physical Games",
    SuggestionCategory.LEARNING        to "📖 Learning",
)

private val timeOfDayEntries: List<Pair<TimeOfDay?, String>> = listOf(
    null                  to "⏰ Any time",
    TimeOfDay.MORNING     to "🌅 Morning",
    TimeOfDay.AFTERNOON   to "☀️ Afternoon",
    TimeOfDay.EVENING     to "🌙 Evening",
    TimeOfDay.NIGHT       to "🌃 Night",
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
    var text         by remember { mutableStateOf(initialText) }
    var category     by remember { mutableStateOf<SuggestionCategory?>(null) }
    var timeOfDay    by remember { mutableStateOf<TimeOfDay?>(null) }
    var textError    by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    // ── Validation ─────────────────────────────────────────────────────────
    fun validate(): Boolean {
        textError = when {
            text.isBlank()   -> "Please enter a suggestion."
            text.length < 5  -> "Too short — be a bit more descriptive."
            text.length > 200 -> "Keep it under 200 characters."
            else             -> null
        }
        categoryError = if (category == null) "Pick a category." else null
        return textError == null && categoryError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add Your Own",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Text input ─────────────────────────────────────────
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it.take(200)
                        if (textError != null) textError = null
                    },
                    label = { Text("What will you do?") },
                    placeholder = { Text("e.g. Make a cup of tea and read a chapter") },
                    isError = textError != null,
                    supportingText = textError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                // ── Category chips ─────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    if (categoryError != null) {
                        Text(
                            text = categoryError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        categoryEntries.forEach { (cat, label) ->
                            val selected = category == cat
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    category = if (selected) null else cat
                                    categoryError = null
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChipSelected,
                                    selectedLabelColor     = ChipSelectedOn,
                                ),
                            )
                        }
                    }
                }

                // ── Time of day chips ──────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Time of day (optional)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        timeOfDayEntries.forEach { (tod, label) ->
                            val selected = timeOfDay == tod
                            FilterChip(
                                selected = selected,
                                onClick  = { timeOfDay = if (selected) null else tod },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChipSelected,
                                    selectedLabelColor     = ChipSelectedOn,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        onSave(
                            AnalogSuggestion(
                                id         = 0,
                                text       = text.trim(),
                                category   = category!!,
                                tags       = emptyList(),
                                timeOfDay  = timeOfDay,
                                isCustom   = true,
                            )
                        )
                    }
                },
            ) {
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

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun CustomSuggestionDialogPreview() {
    BilboTheme {
        CustomSuggestionDialog(
            onSave = {},
            onDismiss = {},
        )
    }
}
