package dev.bilbo.app.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.domain.Emotion

// ── Check-in palette: soft neutral warm tones ─────────────────────────────────
private const val ARGB_BACKGROUND = 0xFF0F1520
private const val ARGB_BACKGROUND_BOT = 0xFF0D1B2A
private const val ARGB_CARD = 0xFF1B2535
private const val ARGB_SELECTED = 0xFF48B8A0
private const val ARGB_SELECTED_BG = 0x2048B8A0
private const val ARGB_ON_SURFACE = 0xFFE0EEF5
private const val ARGB_SUBTLE = 0xFF8AAFC4
private const val ARGB_SURFACE = 0xFF243344

private val CiBackground = Color(ARGB_BACKGROUND) // dark navy background
private val CiBackgroundBot = Color(ARGB_BACKGROUND_BOT) // gradient bottom
private val CiCard = Color(ARGB_CARD) // card surface
private val CiSelected = Color(ARGB_SELECTED) // teal highlight
private val CiSelectedBg = Color(ARGB_SELECTED_BG) // teal tint background
private val CiOnSurface = Color(ARGB_ON_SURFACE) // near-white text
private val CiSubtle = Color(ARGB_SUBTLE) // muted blue
private val CiSurface = Color(ARGB_SURFACE) // slightly lighter surface

private const val GRID_COLUMNS = 2
private const val GRID_SPACING_DP = 12
private const val HEADER_EMOJI_SP = 40
private const val HEADER_LINE_HEIGHT_SP = 34
private const val SCREEN_PADDING_DP = 24
private const val CARD_CORNER_DP = 16
private const val CARD_PAD_V_DP = 20
private const val EMOJI_SP = 30
private const val LABEL_GAP_DP = 6
private const val BORDER_SELECTED_DP = 2
private const val BORDER_DP = 1
private const val ELEVATION_SELECTED_DP = 4
private const val ELEVATION_DP = 0
private const val BORDER_ANIM_MS = 200
private const val SPACE_TOP_DP = 48
private const val SPACE_HEADER_DP = 16
private const val SPACE_SUB_DP = 8
private const val SPACE_GRID_DP = 32
private const val SPACE_SKIP_DP = 28

/**
 * Emotion selection grid inserted between the gatekeeper and app launch.
 *
 * Shows 7 emotion buttons (2 columns), a question prompt, and a Skip option.
 * On selection, calls [onEmotionSelected] and the screen should close/transition.
 *
 * @param onEmotionSelected  Called when the user taps an emotion card.
 * @param onSkip             Called when the user taps "Skip".
 */
@Composable
fun EmotionalCheckInScreen(
    onEmotionSelected: (Emotion) -> Unit,
    onSkip: () -> Unit,
) {
    var selectedEmotion by remember { mutableStateOf<Emotion?>(null) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(CiBackground, CiBackgroundBot),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(SPACE_TOP_DP.dp))
            CheckInHeader()
            Spacer(Modifier.height(SPACE_GRID_DP.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(GRID_SPACING_DP.dp),
                verticalArrangement = Arrangement.spacedBy(GRID_SPACING_DP.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(EMOTION_ITEMS) { item ->
                    EmotionCard(
                        item = item,
                        isSelected = selectedEmotion == item.emotion,
                        onClick = {
                            selectedEmotion = item.emotion
                            onEmotionSelected(item.emotion)
                        },
                    )
                }
            }

            Spacer(Modifier.height(SPACE_SKIP_DP.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = CiSubtle,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }

            Spacer(Modifier.height(SPACE_TOP_DP.dp))
        }
    }
}

@Composable
private fun CheckInHeader() {
    Text(text = "🧠", fontSize = HEADER_EMOJI_SP.sp)

    Spacer(Modifier.height(SPACE_HEADER_DP.dp))

    Text(
        text = "How are you feeling\nright now?",
        style =
            MaterialTheme.typography.headlineSmall.copy(
                color = CiOnSurface,
                fontWeight = FontWeight.SemiBold,
                lineHeight = HEADER_LINE_HEIGHT_SP.sp,
            ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(SPACE_SUB_DP.dp))

    Text(
        text = "Be honest — it helps Bilbo support you better.",
        style = MaterialTheme.typography.bodyMedium.copy(color = CiSubtle),
        textAlign = TextAlign.Center,
    )
}

// ── Emotion card ──────────────────────────────────────────────────────────────

@Composable
private fun emotionCardBackground(isSelected: Boolean): Color {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CiSelectedBg else CiCard,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "EmotionCardBg",
    )
    return bgColor
}

@Composable
private fun emotionCardBorder(isSelected: Boolean): Color {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CiSelected else CiSurface,
        animationSpec = tween(BORDER_ANIM_MS),
        label = "EmotionCardBorder",
    )
    return borderColor
}

@Composable
private fun EmotionCard(
    item: EmotionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = emotionCardBackground(isSelected)
    val borderColor = emotionCardBorder(isSelected)
    val borderWidth = if (isSelected) BORDER_SELECTED_DP.dp else BORDER_DP.dp
    val elevation = if (isSelected) ELEVATION_SELECTED_DP.dp else ELEVATION_DP.dp
    val labelColor = if (isSelected) CiSelected else CiOnSurface
    val labelWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CARD_CORNER_DP.dp))
                .clickable { onClick() }
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(CARD_CORNER_DP.dp),
                ),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = CARD_PAD_V_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = item.emoji, fontSize = EMOJI_SP.sp)
            Spacer(Modifier.height(LABEL_GAP_DP.dp))
            Text(
                text = item.label,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = labelColor,
                        fontWeight = labelWeight,
                    ),
            )
        }
    }
}

// ── Emotion data ──────────────────────────────────────────────────────────────

private data class EmotionItem(
    val emotion: Emotion,
    val emoji: String,
    val label: String,
)

private val EMOTION_ITEMS =
    listOf(
        EmotionItem(Emotion.HAPPY, "😊", "Happy"),
        EmotionItem(Emotion.CALM, "😌", "Calm"),
        EmotionItem(Emotion.BORED, "😑", "Bored"),
        EmotionItem(Emotion.STRESSED, "😫", "Stressed"),
        EmotionItem(Emotion.ANXIOUS, "😰", "Anxious"),
        EmotionItem(Emotion.SAD, "😢", "Sad"),
        EmotionItem(Emotion.LONELY, "😔", "Lonely"),
    )
