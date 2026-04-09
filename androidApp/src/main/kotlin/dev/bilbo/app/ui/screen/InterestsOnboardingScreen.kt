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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.SuggestionCategory

// ── Chip palette ───────────────────────────────────────────────────────────────
private val ChipSelected    = Color(0xFF2D6A4F)
private val ChipSelectedOn  = Color(0xFFD8F3DC)
private val ChipUnselected  = Color(0xFF52B788).copy(alpha = 0.15f)

// ── Minimum selection count ────────────────────────────────────────────────────
private const val MIN_SELECTIONS = 2

// ── Interest definitions ──────────────────────────────────────────────────────

private data class InterestEntry(
    val category: SuggestionCategory,
    val emoji: String,
    val label: String,
)

private val allInterests: List<InterestEntry> = listOf(
    InterestEntry(SuggestionCategory.READING,         "📚", "Reading"),
    InterestEntry(SuggestionCategory.EXERCISE,        "💪", "Exercise"),
    InterestEntry(SuggestionCategory.COOKING,         "🍳", "Cooking"),
    InterestEntry(SuggestionCategory.CREATIVE,        "🎨", "Art"),
    InterestEntry(SuggestionCategory.MUSIC,           "🎵", "Music"),
    InterestEntry(SuggestionCategory.NATURE,          "🌿", "Nature"),
    InterestEntry(SuggestionCategory.SOCIAL,          "👥", "Social"),
    InterestEntry(SuggestionCategory.MINDFULNESS,     "🧘", "Mindfulness"),
    InterestEntry(SuggestionCategory.GAMING_PHYSICAL, "🎲", "Physical Games"),
    InterestEntry(SuggestionCategory.LEARNING,        "📖", "Learning"),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                val selectionCount = selected.size
                val canContinue = selectionCount >= MIN_SELECTIONS

                Button(
                    onClick  = { onContinue(selected) },
                    enabled  = canContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = ChipSelected,
                        contentColor   = ChipSelectedOn,
                    ),
                ) {
                    Text(
                        text = if (canContinue)
                            "Continue ($selectionCount selected)"
                        else
                            "Select at least $MIN_SELECTIONS interests",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ── Heading ───────────────────────────────────────────────
            Text(
                text = "What do you enjoy offline?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We'll suggest activities that match your interests when you feel like reaching for your phone.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Interest chip grid ────────────────────────────────────
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
            ) {
                allInterests.forEach { entry ->
                    val isSelected = entry.category in selected

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) ChipSelected else ChipUnselected,
                        animationSpec = spring(),
                        label = "ChipContainer_${entry.label}",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) ChipSelectedOn
                                      else MaterialTheme.colorScheme.onSurface,
                        animationSpec = spring(),
                        label = "ChipLabel_${entry.label}",
                    )

                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            selected = if (isSelected) selected - entry.category
                                       else selected + entry.category
                        },
                        label = {
                            Text(
                                text  = "${entry.emoji} ${entry.label}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = labelColor,
                                ),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor             = ChipUnselected,
                            selectedContainerColor     = ChipSelected,
                            labelColor                 = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor         = ChipSelectedOn,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun InterestsOnboardingScreenPreview() {
    BilboTheme {
        InterestsOnboardingScreen(
            initialSelections = setOf(
                SuggestionCategory.READING,
                SuggestionCategory.EXERCISE,
                SuggestionCategory.NATURE,
            ),
            onContinue = {},
        )
    }
}
