package dev.spark.app.ui.overlay

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
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
import dev.spark.domain.Emotion

// ── Check-in palette: soft neutral warm tones ─────────────────────────────────
private val CiBackground    = Color(0xFF0F1520)   // dark navy background
private val CiCard          = Color(0xFF1B2535)   // card surface
private val CiSelected      = Color(0xFF48B8A0)   // teal highlight
private val CiSelectedBg    = Color(0x2048B8A0)   // teal tint background
private val CiOnSurface     = Color(0xFFE0EEF5)   // near-white text
private val CiSubtle        = Color(0xFF8AAFC4)   // muted blue
private val CiSurface       = Color(0xFF243344)   // slightly lighter surface

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
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CiBackground, Color(0xFF0D1B2A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(48.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = "🧠",
                fontSize = 40.sp,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "How are you feeling\nright now?",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = CiOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 34.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Be honest — it helps Bilbo support you better.",
                style = MaterialTheme.typography.bodyMedium.copy(color = CiSubtle),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // ── Emotion grid ──────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

            Spacer(Modifier.height(28.dp))

            // ── Skip ──────────────────────────────────────────────────────────
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = CiSubtle,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Emotion card ──────────────────────────────────────────────────────────────

@Composable
private fun EmotionCard(
    item: EmotionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CiSelectedBg else CiCard,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "EmotionCardBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CiSelected else CiSurface,
        animationSpec = tween(200),
        label = "EmotionCardBorder",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.emoji,
                fontSize = 30.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) CiSelected else CiOnSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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

private val EMOTION_ITEMS = listOf(
    EmotionItem(Emotion.HAPPY,   "😊", "Happy"),
    EmotionItem(Emotion.CALM,    "😌", "Calm"),
    EmotionItem(Emotion.BORED,   "😑", "Bored"),
    EmotionItem(Emotion.STRESSED,"😫", "Stressed"),
    EmotionItem(Emotion.ANXIOUS, "😰", "Anxious"),
    EmotionItem(Emotion.SAD,     "😢", "Sad"),
    EmotionItem(Emotion.LONELY,  "😔", "Lonely"),
)
