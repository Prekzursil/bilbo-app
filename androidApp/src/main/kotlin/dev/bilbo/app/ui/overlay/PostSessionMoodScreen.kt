package dev.bilbo.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.domain.Emotion
import kotlinx.coroutines.delay

// ── Post-session palette: compact, warm-neutral ───────────────────────────────
private const val ARGB_BACKGROUND = 0x99000000
private const val ARGB_CARD = 0xFF1E2535
private const val ARGB_ACCENT = 0xFFF5A623
private const val ARGB_ON_SURFACE = 0xFFE0EEF5
private const val ARGB_SUBTLE = 0xFF8AAFC4
private const val ARGB_SURFACE = 0xFF243344

private val PsBackground = Color(ARGB_BACKGROUND) // scrim
private val PsCard = Color(ARGB_CARD) // dark card
private val PsAccent = Color(ARGB_ACCENT) // warm amber
private val PsOnSurface = Color(ARGB_ON_SURFACE) // cool white
private val PsSubtle = Color(ARGB_SUBTLE) // muted blue
private val PsSurface = Color(ARGB_SURFACE) // chip background

private const val AUTO_DISMISS_SECS = 10
private const val TICK_MS = 1_000L
private const val EXIT_DELAY_MS = 300L

private const val CARD_CORNER_DP = 24
private const val CARD_ELEVATION_DP = 16
private const val CARD_PAD_H_DP = 20
private const val CARD_PAD_V_DP = 24
private const val PROGRESS_HEIGHT_DP = 3
private const val PROGRESS_RADIUS_DP = 2
private const val GRID_COLUMNS = 4
private const val GRID_HEIGHT_DP = 130
private const val CHIP_SPACING_DP = 8
private const val CHIP_CORNER_DP = 12
private const val CHIP_PAD_V_DP = 10
private const val CHIP_PAD_H_DP = 4
private const val CHIP_EMOJI_SP = 22
private const val CHIP_LABEL_SP = 10
private const val CHIP_GAP_DP = 3
private const val SPACE_HEADER_DP = 20
private const val SPACE_TINY_DP = 4
private const val SPACE_SUB_DP = 18
private const val SPACE_SKIP_DP = 14
private const val FADE_IN_MS = 250
private const val SLIDE_IN_MS = 340
private const val FADE_OUT_MS = 200
private const val SLIDE_OUT_MS = 260
private const val SLIDE_IN_DIVISOR = 2

/**
 * Compact post-session mood check shown as an overlay when enforcement fires.
 *
 * Presents the same 7 emotions in a smaller format.  Auto-dismisses after
 * [AUTO_DISMISS_SECS] seconds if no selection is made.
 *
 * @param onMoodSelected    Called with the chosen emotion; caller should persist and dismiss.
 * @param onSkip            Called when the user taps "Skip" or the auto-dismiss fires.
 */
@Composable
fun PostSessionMoodScreen(
    onMoodSelected: (Emotion) -> Unit,
    onSkip: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(AUTO_DISMISS_SECS) }
    val progress = countdown.toFloat() / AUTO_DISMISS_SECS.toFloat()

    // Slide in on appear
    LaunchedEffect(Unit) { visible = true }

    // Auto-dismiss countdown
    LaunchedEffect(Unit) {
        repeat(AUTO_DISMISS_SECS) {
            delay(TICK_MS)
            countdown--
        }
        visible = false
        delay(EXIT_DELAY_MS)
        onSkip()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PsBackground),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(tween(FADE_IN_MS)) +
                    slideInVertically(
                        initialOffsetY = { it / SLIDE_IN_DIVISOR },
                        animationSpec = tween(SLIDE_IN_MS),
                    ),
            exit =
                fadeOut(tween(FADE_OUT_MS)) +
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(SLIDE_OUT_MS),
                    ),
        ) {
            MoodCard(
                progress = progress,
                countdown = countdown,
                onMoodSelected = { emotion ->
                    visible = false
                    onMoodSelected(emotion)
                },
                onSkip = {
                    visible = false
                    onSkip()
                },
            )
        }
    }
}

@Composable
private fun MoodCard(
    progress: Float,
    countdown: Int,
    onMoodSelected: (Emotion) -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = CARD_CORNER_DP.dp, topEnd = CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = PsCard),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CARD_PAD_H_DP.dp, vertical = CARD_PAD_V_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoodCardHeader(progress = progress, countdown = countdown)

            Spacer(Modifier.height(SPACE_SUB_DP.dp))

            MoodGrid(onMoodSelected = onMoodSelected)

            Spacer(Modifier.height(SPACE_SKIP_DP.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PsSubtle),
                )
            }
        }
    }
}

@Composable
private fun MoodCardHeader(
    progress: Float,
    countdown: Int,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PROGRESS_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(PROGRESS_RADIUS_DP.dp)),
        color = PsAccent,
        trackColor = PsSurface,
        strokeCap = StrokeCap.Round,
    )

    Spacer(Modifier.height(SPACE_HEADER_DP.dp))

    Text(
        text = "How do you feel now?",
        style =
            MaterialTheme.typography.titleMedium.copy(
                color = PsOnSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(SPACE_TINY_DP.dp))

    Text(
        text = "Auto-dismisses in $countdown s",
        style = MaterialTheme.typography.labelSmall.copy(color = PsSubtle),
    )
}

@Composable
private fun MoodGrid(onMoodSelected: (Emotion) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(CHIP_SPACING_DP.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(GRID_HEIGHT_DP.dp),
    ) {
        items(EMOTION_ITEMS_COMPACT) { item ->
            CompactEmotionChip(
                emoji = item.emoji,
                label = item.label,
                onClick = { onMoodSelected(item.emotion) },
            )
        }
    }
}

// ── Compact emotion chip ──────────────────────────────────────────────────────

@Composable
private fun CompactEmotionChip(
    emoji: String,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(CHIP_CORNER_DP.dp))
                .background(PsSurface)
                .clickable { onClick() }
                .padding(vertical = CHIP_PAD_V_DP.dp, horizontal = CHIP_PAD_H_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = CHIP_EMOJI_SP.sp)
        Spacer(Modifier.height(CHIP_GAP_DP.dp))
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color = PsOnSurface,
                    fontSize = CHIP_LABEL_SP.sp,
                ),
            maxLines = 1,
        )
    }
}

// ── Emotion data ──────────────────────────────────────────────────────────────

private data class CompactEmotionItem(
    val emotion: Emotion,
    val emoji: String,
    val label: String,
)

private val EMOTION_ITEMS_COMPACT =
    listOf(
        CompactEmotionItem(Emotion.HAPPY, "😊", "Happy"),
        CompactEmotionItem(Emotion.CALM, "😌", "Calm"),
        CompactEmotionItem(Emotion.BORED, "😑", "Bored"),
        CompactEmotionItem(Emotion.STRESSED, "😫", "Stressed"),
        CompactEmotionItem(Emotion.ANXIOUS, "😰", "Anxious"),
        CompactEmotionItem(Emotion.SAD, "😢", "Sad"),
        CompactEmotionItem(Emotion.LONELY, "😔", "Lonely"),
    )
