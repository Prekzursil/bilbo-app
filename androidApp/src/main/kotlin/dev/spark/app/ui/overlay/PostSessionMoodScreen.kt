package dev.spark.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import dev.spark.domain.Emotion
import kotlinx.coroutines.delay

// ── Post-session palette: compact, warm-neutral ───────────────────────────────
private val PsBackground  = Color(0x99000000)   // scrim
private val PsCard        = Color(0xFF1E2535)   // dark card
private val PsAccent      = Color(0xFFF5A623)   // warm amber
private val PsOnSurface   = Color(0xFFE0EEF5)   // cool white
private val PsSubtle      = Color(0xFF8AAFC4)   // muted blue
private val PsSurface     = Color(0xFF243344)   // chip background
private val PsSelected    = Color(0xFF48B8A0)   // selected teal

private const val AUTO_DISMISS_SECS = 10

/**
 * Compact post-session mood check shown as an overlay when enforcement fires.
 *
 * Presents the same 7 emotions in a smaller format.  Auto-dismisses after
 * [AUTO_DISMISS_SECS] seconds if no selection is made.
 *
 * @param checkInId         The [EmotionalCheckIn.id] whose postSessionMood to update.
 * @param onMoodSelected    Called with the chosen emotion; caller should persist and dismiss.
 * @param onSkip            Called when the user taps "Skip" or the auto-dismiss fires.
 */
@Composable
fun PostSessionMoodScreen(
    checkInId: Long,
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
            delay(1_000L)
            countdown--
        }
        visible = false
        delay(300L)
        onSkip()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PsBackground),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(250)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(340),
            ),
            exit = fadeOut(tween(200)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(260),
            ),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = PsCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    // ── Auto-dismiss progress ─────────────────────────────────
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PsAccent,
                        trackColor = PsSurface,
                        strokeCap = StrokeCap.Round,
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Header ────────────────────────────────────────────────
                    Text(
                        text = "How do you feel now?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = PsOnSurface,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Auto-dismisses in $countdown s",
                        style = MaterialTheme.typography.labelSmall.copy(color = PsSubtle),
                    )

                    Spacer(Modifier.height(18.dp))

                    // ── Compact emotion grid (3 columns) ──────────────────────
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp), // fixed height for 2 rows
                    ) {
                        items(EMOTION_ITEMS_COMPACT) { item ->
                            CompactEmotionChip(
                                emoji = item.emoji,
                                label = item.label,
                                onClick = {
                                    visible = false
                                    onMoodSelected(item.emotion)
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    TextButton(
                        onClick = {
                            visible = false
                            onSkip()
                        },
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.bodyMedium.copy(color = PsSubtle),
                        )
                    }
                }
            }
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
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PsSurface)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = PsOnSurface,
                fontSize = 10.sp,
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

private val EMOTION_ITEMS_COMPACT = listOf(
    CompactEmotionItem(Emotion.HAPPY,   "😊", "Happy"),
    CompactEmotionItem(Emotion.CALM,    "😌", "Calm"),
    CompactEmotionItem(Emotion.BORED,   "😑", "Bored"),
    CompactEmotionItem(Emotion.STRESSED,"😫", "Stressed"),
    CompactEmotionItem(Emotion.ANXIOUS, "😰", "Anxious"),
    CompactEmotionItem(Emotion.SAD,     "😢", "Sad"),
    CompactEmotionItem(Emotion.LONELY,  "😔", "Lonely"),
)
