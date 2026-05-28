package dev.bilbo.app.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bilbo.domain.SuggestionCategory

// ── Chip palette ───────────────────────────────────────────────────────────────
private const val ARGB_CHIP_SELECTED = 0xFF2D6A4F
private const val ARGB_CHIP_SELECTED_ON = 0xFFD8F3DC
private const val ARGB_CHIP_UNSELECTED = 0xFF52B788
private const val ALPHA_CHIP_UNSELECTED = 0.15f

private val ChipSelected = Color(ARGB_CHIP_SELECTED)
private val ChipSelectedOn = Color(ARGB_CHIP_SELECTED_ON)
private val ChipUnselected = Color(ARGB_CHIP_UNSELECTED).copy(alpha = ALPHA_CHIP_UNSELECTED)

// ── Minimum selection count ────────────────────────────────────────────────────
private const val MIN_SELECTIONS = 2
private const val CHIP_GAP_DP = 10
private const val CHIP_CORNER_DP = 12
private const val BUTTON_CORNER_DP = 14
private const val SCREEN_PAD_H_DP = 24
private const val BOTTOM_PAD_V_DP = 20
private const val SPACE_TOP_DP = 40
private const val SPACE_TITLE_DP = 8
private const val SPACE_GRID_DP = 32
private const val SPACE_BOTTOM_DP = 24

// ── Interest definitions ──────────────────────────────────────────────────────

private data class InterestEntry(
    val category: SuggestionCategory,
    val emoji: String,
    val label: String,
)

private val allInterests: List<InterestEntry> =
    listOf(
        InterestEntry(SuggestionCategory.READING, "📚", "Reading"),
        InterestEntry(SuggestionCategory.EXERCISE, "💪", "Exercise"),
        InterestEntry(SuggestionCategory.COOKING, "🍳", "Cooking"),
        InterestEntry(SuggestionCategory.CREATIVE, "🎨", "Art"),
        InterestEntry(SuggestionCategory.MUSIC, "🎵", "Music"),
        InterestEntry(SuggestionCategory.NATURE, "🌿", "Nature"),
        InterestEntry(SuggestionCategory.SOCIAL, "👥", "Social"),
        InterestEntry(SuggestionCategory.MINDFULNESS, "🧘", "Mindfulness"),
        InterestEntry(SuggestionCategory.GAMING_PHYSICAL, "🎲", "Physical Games"),
        InterestEntry(SuggestionCategory.LEARNING, "📖", "Learning"),
    )

/**
 * First-run onboarding screen for selecting offline interests.
 *
 * Displays a grid of selectable [FilterChip] items — one per [SuggestionCategory].
 * The "Continue" button is enabled only when at least [MIN_SELECTIONS] interests
 * are selected.
 *
 * @param initialSelections Categories pre-selected (empty on first run).
 * @param onContinue        Invoked with the final set of selected categories.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InterestsOnboardingScreen(
    initialSelections: Set<SuggestionCategory> = emptySet(),
    onContinue: (Set<SuggestionCategory>) -> Unit,
) {
    var selected by remember {
        mutableStateOf(initialSelections.toSet())
    }

    Scaffold(
        bottomBar = {
            InterestsContinueBar(
                selectionCount = selected.size,
                onContinue = { onContinue(selected) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SCREEN_PAD_H_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(SPACE_TOP_DP.dp))
            InterestsHeading()
            Spacer(modifier = Modifier.height(SPACE_GRID_DP.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CHIP_GAP_DP.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(CHIP_GAP_DP.dp),
            ) {
                allInterests.forEach { entry ->
                    InterestChip(
                        entry = entry,
                        isSelected = entry.category in selected,
                        onToggle = {
                            selected =
                                if (entry.category in selected) {
                                    selected - entry.category
                                } else {
                                    selected + entry.category
                                }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(SPACE_BOTTOM_DP.dp))
        }
    }
}

@Composable
private fun InterestsContinueBar(
    selectionCount: Int,
    onContinue: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SCREEN_PAD_H_DP.dp, vertical = BOTTOM_PAD_V_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        val canContinue = selectionCount >= MIN_SELECTIONS
        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = ChipSelected,
                    contentColor = ChipSelectedOn,
                ),
        ) {
            Text(
                text =
                    if (canContinue) {
                        "Continue ($selectionCount selected)"
                    } else {
                        "Select at least $MIN_SELECTIONS interests"
                    },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun InterestsHeading() {
    Text(
        text = "What do you enjoy offline?",
        style =
            MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
    )
    Spacer(modifier = Modifier.height(SPACE_TITLE_DP.dp))
    Text(
        text = "We'll suggest activities that match your interests when you feel like reaching for your phone.",
        style =
            MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InterestChip(
    entry: InterestEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) ChipSelectedOn else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(),
        label = "ChipLabel_${entry.label}",
    )
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = {
            Text(
                text = "${entry.emoji} ${entry.label}",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                    ),
            )
        },
        shape = RoundedCornerShape(CHIP_CORNER_DP.dp),
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = ChipUnselected,
                selectedContainerColor = ChipSelected,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = ChipSelectedOn,
            ),
    )
}
