package dev.bilbo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay

// ── Nature-inspired green palette ─────────────────────────────────────────────
private val CardGreen       = Color(0xFF2D6A4F)
private val CardGreenLight  = Color(0xFF52B788)
private val CardGreenSurface= Color(0xFF40916C)
private val CardOnGreen     = Color(0xFFD8F3DC)
private val CardSubtle      = Color(0xFFB7E4C7)

// ── Category emoji map ────────────────────────────────────────────────────────
private fun SuggestionCategory.emoji(): String = when (this) {
    SuggestionCategory.EXERCISE          -> "💪"
    SuggestionCategory.CREATIVE          -> "🎨"
    SuggestionCategory.SOCIAL            -> "👥"
    SuggestionCategory.MINDFULNESS       -> "🧘"
    SuggestionCategory.LEARNING          -> "📖"
    SuggestionCategory.NATURE            -> "🌿"
    SuggestionCategory.COOKING           -> "🍳"
    SuggestionCategory.MUSIC             -> "🎵"
    SuggestionCategory.GAMING_PHYSICAL   -> "🎲"
    SuggestionCategory.READING           -> "📚"
}

private fun SuggestionCategory.label(): String = when (this) {
    SuggestionCategory.EXERCISE          -> "Exercise"
    SuggestionCategory.CREATIVE          -> "Creative"
    SuggestionCategory.SOCIAL            -> "Social"
    SuggestionCategory.MINDFULNESS       -> "Mindfulness"
    SuggestionCategory.LEARNING          -> "Learning"
    SuggestionCategory.NATURE            -> "Nature"
    SuggestionCategory.COOKING           -> "Cooking"
    SuggestionCategory.MUSIC             -> "Music"
    SuggestionCategory.GAMING_PHYSICAL   -> "Physical Games"
    SuggestionCategory.READING           -> "Reading"
}

/**
 * A card that displays a single [AnalogSuggestion].
 *
 * When the user taps "Show another", the card performs a 3-D flip animation
 * before [onShowAnother] is invoked, giving the caller time to swap the data.
 *
 * @param suggestion   The suggestion to display.
 * @param onAccept     Called when the user taps "I'll do this! (+5 FP)".
 * @param onShowAnother Called when the user taps "Show another".
 */
@Composable
fun AnalogSuggestionCard(
    suggestion: AnalogSuggestion,
    onAccept: () -> Unit,
    onShowAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flip animation state.  flipping = true → card rotates 90° (face hides) → callback fires.
    var flipping by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (flipping) 90f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        finishedListener = { angle ->
            if (angle >= 89f) {
                // Half-way through: invoke callback, then animate back from -90° to 0°
                flipping = false
                onShowAnother()
            }
        },
        label = "CardFlip",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Category badge ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CardGreenLight.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = suggestion.category.emoji(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = suggestion.category.label(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = CardSubtle,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                suggestion.timeOfDay?.let { tod ->
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = tod.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(color = CardSubtle),
                    )
                }
            }

            // ── Suggestion text ───────────────────────────────────────────
            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = CardOnGreen,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Action buttons ────────────────────────────────────────────
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardGreenLight,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "I'll do this! (+5 FP)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            TextButton(
                onClick = { flipping = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Show another",
                    style = MaterialTheme.typography.labelMedium.copy(color = CardSubtle),
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun AnalogSuggestionCardPreview() {
    BilboTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AnalogSuggestionCard(
                suggestion = AnalogSuggestion(
                    id = 1,
                    text = "Step outside for a 10-minute walk around the block.",
                    category = SuggestionCategory.EXERCISE,
                    tags = listOf("outdoors", "quick"),
                    timeOfDay = TimeOfDay.MORNING,
                ),
                onAccept = {},
                onShowAnother = {},
            )
        }
    }
}
