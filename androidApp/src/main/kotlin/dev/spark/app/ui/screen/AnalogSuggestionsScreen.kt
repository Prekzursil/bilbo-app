package dev.spark.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.spark.app.ui.components.AnalogSuggestionCard
import dev.spark.app.ui.components.CustomSuggestionDialog
import dev.spark.app.ui.theme.BilboTheme
import dev.spark.domain.AnalogSuggestion
import dev.spark.domain.SuggestionCategory
import dev.spark.domain.TimeOfDay

// ── Palette ────────────────────────────────────────────────────────────────────
private val GreenDeep   = Color(0xFF2D6A4F)
private val GreenLight  = Color(0xFF52B788)
private val GreenSurface = Color(0xFF1B4332)

// ── UI State ──────────────────────────────────────────────────────────────────

data class AnalogSuggestionsUiState(
    /** Active suggestions shown at the top (up to 3). */
    val activeSuggestions: List<AnalogSuggestion> = emptyList(),
    /** User-created custom suggestions shown in the list below. */
    val customSuggestions: List<AnalogSuggestion> = emptyList(),
    val isLoading: Boolean = false,
)

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Full screen for browsing and managing analog suggestions.
 *
 * Layout:
 *  - "Need inspiration?" header
 *  - Up to 3 [AnalogSuggestionCard] items
 *  - "Your Custom Suggestions" section
 *  - FAB → [CustomSuggestionDialog]
 *
 * @param uiState          Current screen state.
 * @param onAccept         Called when user accepts a suggestion (id).
 * @param onShowAnother    Called when user requests a new card (id to replace).
 * @param onAddCustom      Called with the new suggestion data.
 * @param onDeleteCustom   Called when user deletes a custom suggestion (id).
 * @param onBack           Optional back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalogSuggestionsScreen(
    uiState: AnalogSuggestionsUiState,
    onAccept: (Long) -> Unit,
    onShowAnother: (Long) -> Unit,
    onAddCustom: (AnalogSuggestion) -> Unit,
    onDeleteCustom: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analog Alternatives") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = GreenLight,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add your own")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────
            item {
                InspirationHeader()
            }

            // ── Active suggestion cards ───────────────────────────────
            itemsIndexed(uiState.activeSuggestions) { index, suggestion ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                ) {
                    AnalogSuggestionCard(
                        suggestion = suggestion,
                        onAccept   = { onAccept(suggestion.id) },
                        onShowAnother = { onShowAnother(suggestion.id) },
                    )
                }
            }

            // ── Custom suggestions section ───────────────────────────
            if (uiState.customSuggestions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Custom Suggestions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                itemsIndexed(uiState.customSuggestions) { _, suggestion ->
                    CustomSuggestionRow(
                        suggestion = suggestion,
                        onDelete   = { onDeleteCustom(suggestion.id) },
                    )
                }
            }

            // Bottom spacer for FAB clearance
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ── Add dialog ────────────────────────────────────────────────────────
    if (showAddDialog) {
        CustomSuggestionDialog(
            onSave   = { newSuggestion ->
                onAddCustom(newSuggestion)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun InspirationHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = "Need inspiration?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Step away from the screen — here are some ideas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Custom suggestion row ─────────────────────────────────────────────────────

@Composable
private fun CustomSuggestionRow(
    suggestion: AnalogSuggestion,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Category emoji
            Text(
                text = suggestion.category.emoji(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.size(28.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                Text(
                    text = suggestion.category.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Category helpers (duplicated from card to avoid cross-component coupling) ──

private fun SuggestionCategory.emoji(): String = when (this) {
    SuggestionCategory.EXERCISE        -> "💪"
    SuggestionCategory.CREATIVE        -> "🎨"
    SuggestionCategory.SOCIAL          -> "👥"
    SuggestionCategory.MINDFULNESS     -> "🧘"
    SuggestionCategory.LEARNING        -> "📖"
    SuggestionCategory.NATURE          -> "🌿"
    SuggestionCategory.COOKING         -> "🍳"
    SuggestionCategory.MUSIC           -> "🎵"
    SuggestionCategory.GAMING_PHYSICAL -> "🎲"
    SuggestionCategory.READING         -> "📚"
}

private fun SuggestionCategory.label(): String = when (this) {
    SuggestionCategory.EXERCISE        -> "Exercise"
    SuggestionCategory.CREATIVE        -> "Creative"
    SuggestionCategory.SOCIAL          -> "Social"
    SuggestionCategory.MINDFULNESS     -> "Mindfulness"
    SuggestionCategory.LEARNING        -> "Learning"
    SuggestionCategory.NATURE          -> "Nature"
    SuggestionCategory.COOKING         -> "Cooking"
    SuggestionCategory.MUSIC           -> "Music"
    SuggestionCategory.GAMING_PHYSICAL -> "Physical Games"
    SuggestionCategory.READING         -> "Reading"
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AnalogSuggestionsScreenPreview() {
    BilboTheme {
        AnalogSuggestionsScreen(
            uiState = AnalogSuggestionsUiState(
                activeSuggestions = listOf(
                    AnalogSuggestion(
                        id = 1,
                        text = "Step outside for a 10-minute walk around the block.",
                        category = SuggestionCategory.EXERCISE,
                        tags = listOf("outdoors", "quick"),
                        timeOfDay = TimeOfDay.MORNING,
                    ),
                    AnalogSuggestion(
                        id = 2,
                        text = "Make a cup of tea and read for 15 minutes.",
                        category = SuggestionCategory.READING,
                        tags = listOf("calm", "cozy"),
                        timeOfDay = null,
                    ),
                    AnalogSuggestion(
                        id = 3,
                        text = "Sketch something from memory — no references.",
                        category = SuggestionCategory.CREATIVE,
                        tags = listOf("art", "creative"),
                        timeOfDay = TimeOfDay.EVENING,
                    ),
                ),
                customSuggestions = listOf(
                    AnalogSuggestion(
                        id = 100,
                        text = "Water the plants and tidy the windowsill.",
                        category = SuggestionCategory.NATURE,
                        tags = listOf("home", "plants"),
                        isCustom = true,
                    ),
                ),
            ),
            onAccept = {},
            onShowAnother = {},
            onAddCustom = {},
            onDeleteCustom = {},
        )
    }
}
