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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.components.SuggestionVisuals.emoji
import dev.bilbo.app.ui.components.SuggestionVisuals.label
import dev.bilbo.domain.AnalogSuggestion

private const val ARGB_CARD_GREEN = 0xFF2D6A4F
private const val ARGB_CARD_GREEN_LIGHT = 0xFF52B788
private const val ARGB_CARD_ON_GREEN = 0xFFD8F3DC
private const val ARGB_CARD_SUBTLE = 0xFFB7E4C7

private const val FLIP_ANGLE = 90f
private const val FLIP_DONE_ANGLE = 89f
private const val FLIP_DURATION_MS = 220
private const val CAMERA_DISTANCE = 12f
private const val ALPHA_BADGE_BG = 0.3f

private const val RADIUS_HERO_DP = 20
private const val RADIUS_BUTTON_DP = 12
private const val ELEVATION_DP = 4
private const val PADDING_DP = 20
private const val SPACE_LARGE_DP = 12
private const val SPACE_MEDIUM_DP = 8
private const val SPACE_SMALL_DP = 4
private const val BADGE_SIZE_DP = 36

// ── Nature-inspired green palette ─────────────────────────────────────────────
private val CardGreen = Color(ARGB_CARD_GREEN)
private val CardGreenLight = Color(ARGB_CARD_GREEN_LIGHT)
private val CardOnGreen = Color(ARGB_CARD_ON_GREEN)
private val CardSubtle = Color(ARGB_CARD_SUBTLE)

/**
 * A card that displays a single [AnalogSuggestion].
 *
 * When the user taps "Show another", the card performs a 3-D flip animation
 * before [onShowAnother] is invoked, giving the caller time to swap the data.
 *
 * @param suggestion    The suggestion to display.
 * @param onAccept      Called when the user taps "I'll do this! (+5 FP)".
 * @param onShowAnother Called when the user taps "Show another".
 * @param modifier      Layout modifier applied to the card.
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
        targetValue = if (flipping) FLIP_ANGLE else 0f,
        animationSpec = tween(durationMillis = FLIP_DURATION_MS, easing = FastOutSlowInEasing),
        finishedListener = { angle ->
            if (angle >= FLIP_DONE_ANGLE) {
                // Half-way through: invoke callback, then animate back from -90° to 0°
                flipping = false
                onShowAnother()
            }
        },
        label = "CardFlip",
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationX = rotation
                    cameraDistance = CAMERA_DISTANCE * density
                },
        shape = RoundedCornerShape(RADIUS_HERO_DP.dp),
        colors = CardDefaults.cardColors(containerColor = CardGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION_DP.dp),
    ) {
        Column(
            modifier = Modifier.padding(PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
        ) {
            CategoryBadge(suggestion)
            Text(
                text = suggestion.text,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = CardOnGreen,
                        fontWeight = FontWeight.Medium,
                    ),
            )
            Spacer(modifier = Modifier.height(SPACE_SMALL_DP.dp))
            ActionButtons(onAccept = onAccept, onShowAnother = { flipping = true })
        }
    }
}

@Composable
private fun CategoryBadge(suggestion: AnalogSuggestion) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACE_MEDIUM_DP.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(BADGE_SIZE_DP.dp)
                    .background(CardGreenLight.copy(alpha = ALPHA_BADGE_BG), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = suggestion.category.emoji(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = suggestion.category.label(),
            style =
                MaterialTheme.typography.labelMedium.copy(
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
}

@Composable
private fun ActionButtons(
    onAccept: () -> Unit,
    onShowAnother: () -> Unit,
) {
    Button(
        onClick = onAccept,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RADIUS_BUTTON_DP.dp),
        colors =
            ButtonDefaults.buttonColors(
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
        onClick = onShowAnother,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Show another",
            style = MaterialTheme.typography.labelMedium.copy(color = CardSubtle),
        )
    }
}
