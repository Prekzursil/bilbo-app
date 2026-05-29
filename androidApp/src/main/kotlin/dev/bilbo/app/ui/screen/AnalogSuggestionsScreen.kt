package dev.bilbo.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.components.AnalogSuggestionCard
import dev.bilbo.app.ui.components.CustomSuggestionDialog
import dev.bilbo.app.ui.components.SuggestionVisuals.emoji
import dev.bilbo.app.ui.components.SuggestionVisuals.label
import dev.bilbo.domain.AnalogSuggestion

private const val ARGB_GREEN_LIGHT = 0xFF52B788
private const val SCREEN_PADDING_H_DP = 16
private const val SCREEN_PADDING_V_DP = 8
private const val SECTION_SPACE_DP = 16
private const val SPACE_SMALL_DP = 4
private const val SPACE_MEDIUM_DP = 8
private const val SPACE_LARGE_DP = 12
private const val FAB_CLEARANCE_DP = 80
private const val ROW_RADIUS_DP = 14
private const val ROW_PADDING_H_DP = 16
private const val ROW_PADDING_V_DP = 12
private const val EMOJI_SIZE_DP = 28
private const val TEXT_MAX_LINES = 2

// ── Palette ────────────────────────────────────────────────────────────────────
private val GreenLight = Color(ARGB_GREEN_LIGHT)

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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(horizontal = SCREEN_PADDING_H_DP.dp, vertical = SCREEN_PADDING_V_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACE_DP.dp),
        ) {
            item { InspirationHeader() }
            activeSuggestionItems(uiState.activeSuggestions, onAccept, onShowAnother)
            customSuggestionItems(uiState.customSuggestions, onDeleteCustom)
            item { Spacer(modifier = Modifier.height(FAB_CLEARANCE_DP.dp)) }
        }
    }

    // ── Add dialog ────────────────────────────────────────────────────────
    if (showAddDialog) {
        CustomSuggestionDialog(
            onSave = { newSuggestion ->
                onAddCustom(newSuggestion)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

private fun LazyListScope.activeSuggestionItems(
    suggestions: List<AnalogSuggestion>,
    onAccept: (Long) -> Unit,
    onShowAnother: (Long) -> Unit,
) {
    itemsIndexed(suggestions) { _, suggestion ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        ) {
            AnalogSuggestionCard(
                suggestion = suggestion,
                onAccept = { onAccept(suggestion.id) },
                onShowAnother = { onShowAnother(suggestion.id) },
            )
        }
    }
}

private fun LazyListScope.customSuggestionItems(
    suggestions: List<AnalogSuggestion>,
    onDeleteCustom: (Long) -> Unit,
) {
    if (suggestions.isEmpty()) return
    item {
        Spacer(modifier = Modifier.height(SPACE_MEDIUM_DP.dp))
        Text(
            text = "Your Custom Suggestions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(vertical = SPACE_SMALL_DP.dp),
        )
    }
    itemsIndexed(suggestions) { _, suggestion ->
        CustomSuggestionRow(
            suggestion = suggestion,
            onDelete = { onDeleteCustom(suggestion.id) },
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun InspirationHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        Text(
            text = "Need inspiration?",
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
        )
        Spacer(modifier = Modifier.height(SPACE_SMALL_DP.dp))
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
        shape = RoundedCornerShape(ROW_RADIUS_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ROW_PADDING_H_DP.dp, vertical = ROW_PADDING_V_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
        ) {
            // Category emoji
            Text(
                text = suggestion.category.emoji(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.size(EMOJI_SIZE_DP.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = TEXT_MAX_LINES,
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
