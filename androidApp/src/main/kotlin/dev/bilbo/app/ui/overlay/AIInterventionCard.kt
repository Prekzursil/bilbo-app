package dev.bilbo.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.domain.Emotion

// ── Intervention palette: warm, supportive, non-judgmental ────────────────────
private const val ARGB_BACKGROUND = 0x80000000
private const val ARGB_CARD = 0xFF1E1528
private const val ARGB_PURPLE = 0xFFB08DFF
private const val ARGB_PURPLE_DIM = 0xFF7C5CBF
private const val ARGB_ON_SURFACE = 0xFFEDE8F8
private const val ARGB_SUBTLE = 0xFF998AB8
private const val ARGB_GREEN = 0xFF6BCB77
private const val ARGB_NEUTRAL = 0xFF5A5A6A
private const val ARGB_BREATHE_LABEL = 0xFF001A0D

private val AiBackground = Color(ARGB_BACKGROUND) // scrim
private val AiCard = Color(ARGB_CARD) // deep warm violet-grey
private val AiPurple = Color(ARGB_PURPLE) // soft lilac accent
private val AiPurpleDim = Color(ARGB_PURPLE_DIM) // dim purple
private val AiOnSurface = Color(ARGB_ON_SURFACE) // lavender-white
private val AiSubtle = Color(ARGB_SUBTLE) // muted lavender
private val AiGreen = Color(ARGB_GREEN) // breathe button
private val AiNeutral = Color(ARGB_NEUTRAL) // continue button outline

private const val FADE_IN_MS = 280
private const val SLIDE_IN_MS = 360
private const val SLIDE_IN_DIVISOR = 3
private const val ICON_SP = 36
private const val CARD_PAD_OUTER_DP = 20
private const val CARD_CORNER_DP = 24
private const val CARD_ELEVATION_DP = 12
private const val CARD_PAD_DP = 28
private const val DIVIDER_THICKNESS_DP = 1
private const val ALPHA_DIVIDER = 0.3f
private const val SUGGESTION_LINE_HEIGHT_SP = 24
private const val PATTERN_LINE_HEIGHT_SP = 22
private const val BREATHE_BUTTON_HEIGHT_DP = 54
private const val CONTINUE_BUTTON_HEIGHT_DP = 48
private const val BUTTON_CORNER_DP = 14
private const val BORDER_THIN_DP = 1
private const val ALPHA_CONTINUE_BORDER = 0.5f
private const val SPACE_ICON_DP = 16
private const val SPACE_SECTION_DP = 20
private const val SPACE_SUGGESTION_DP = 24
private const val SPACE_BETWEEN_DP = 10

/**
 * Full-overlay card shown after a negative emotion check-in on an Empty Calorie app.
 *
 * Presents a gentle pattern observation (emotion → app → typical duration → post-mood)
 * and offers the user a choice between a 2-minute breathing exercise or continuing.
 *
 * @param emotion         The emotion the user just declared.
 * @param appName         The Empty Calorie app they're about to open.
 * @param avgDurationMins Typical minutes spent in this app during this emotional state.
 * @param postMood        The emotion typically felt afterward (from HeuristicEngine data).
 * @param onBreathe       User chose the breathing exercise path.
 * @param onContinue      User chose to proceed to the app normally.
 */
@Composable
fun AIInterventionCard(
    emotion: Emotion,
    appName: String,
    avgDurationMins: Int,
    postMood: Emotion?,
    onBreathe: () -> Unit,
    onContinue: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AiBackground),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(tween(FADE_IN_MS)) +
                    slideInVertically(
                        initialOffsetY = { it / SLIDE_IN_DIVISOR },
                        animationSpec = tween(SLIDE_IN_MS),
                    ),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = CARD_PAD_OUTER_DP.dp),
                shape = RoundedCornerShape(CARD_CORNER_DP.dp),
                colors = CardDefaults.cardColors(containerColor = AiCard),
                elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
            ) {
                InterventionCardContent(
                    emotion = emotion,
                    appName = appName,
                    avgDurationMins = avgDurationMins,
                    postMood = postMood,
                    onBreathe = onBreathe,
                    onContinue = onContinue,
                )
            }
        }
    }
}

@Composable
private fun InterventionCardContent(
    emotion: Emotion,
    appName: String,
    avgDurationMins: Int,
    postMood: Emotion?,
    onBreathe: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(CARD_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "✨", fontSize = ICON_SP.sp)
        Spacer(Modifier.height(SPACE_ICON_DP.dp))
        PatternObservationText(
            emotion = emotion,
            appName = appName,
            avgDurationMins = avgDurationMins,
            postMood = postMood,
        )
        Spacer(Modifier.height(SPACE_SECTION_DP.dp))
        HorizontalDivider(
            color = AiPurpleDim.copy(alpha = ALPHA_DIVIDER),
            thickness = DIVIDER_THICKNESS_DP.dp,
        )
        Spacer(Modifier.height(SPACE_SECTION_DP.dp))
        Text(
            text = "Would you like to try 2 minutes\nof breathing instead?",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = AiOnSurface,
                    fontWeight = FontWeight.Medium,
                    lineHeight = SUGGESTION_LINE_HEIGHT_SP.sp,
                ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SPACE_SUGGESTION_DP.dp))
        InterventionActions(appName = appName, onBreathe = onBreathe, onContinue = onContinue)
    }
}

@Composable
private fun InterventionActions(
    appName: String,
    onBreathe: () -> Unit,
    onContinue: () -> Unit,
) {
    Button(
        onClick = onBreathe,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BREATHE_BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AiGreen),
    ) {
        Text(
            text = "Yes, let me breathe 🌬️",
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(ARGB_BREATHE_LABEL),
                ),
        )
    }

    Spacer(Modifier.height(SPACE_BETWEEN_DP.dp))

    OutlinedButton(
        onClick = onContinue,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CONTINUE_BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AiSubtle),
        border =
            androidx.compose.foundation.BorderStroke(
                BORDER_THIN_DP.dp,
                AiNeutral.copy(alpha = ALPHA_CONTINUE_BORDER),
            ),
    ) {
        Text(
            text = "Continue to $appName",
            style = MaterialTheme.typography.bodyMedium.copy(color = AiSubtle),
        )
    }
}

// ── Pattern text builder ──────────────────────────────────────────────────────

@Composable
private fun PatternObservationText(
    emotion: Emotion,
    appName: String,
    avgDurationMins: Int,
    postMood: Emotion?,
) {
    val emotionLabel = emotion.displayLabel()
    val postMoodLabel = postMood?.displayLabel()

    Text(
        text =
            buildAnnotatedString {
                append("When you feel ")
                withStyle(SpanStyle(color = AiPurple, fontWeight = FontWeight.SemiBold)) {
                    append(emotionLabel)
                }
                append(", you tend to use ")
                withStyle(SpanStyle(color = AiPurple, fontWeight = FontWeight.SemiBold)) {
                    append(appName)
                }
                append(" for about ")
                withStyle(SpanStyle(color = AiPurple, fontWeight = FontWeight.SemiBold)) {
                    append("$avgDurationMins min")
                }
                append(".")

                if (postMoodLabel != null) {
                    append(" Afterward you usually feel ")
                    withStyle(SpanStyle(color = AiPurple, fontWeight = FontWeight.SemiBold)) {
                        append(postMoodLabel)
                    }
                    append(".")
                }
            },
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color = AiSubtle,
                lineHeight = PATTERN_LINE_HEIGHT_SP.sp,
            ),
        textAlign = TextAlign.Center,
    )
}

// ── Emotion display helpers ───────────────────────────────────────────────────

private fun Emotion.displayLabel(): String =
    when (this) {
        Emotion.HAPPY -> "happy 😊"
        Emotion.CALM -> "calm 😌"
        Emotion.BORED -> "bored 😑"
        Emotion.STRESSED -> "stressed 😫"
        Emotion.ANXIOUS -> "anxious 😰"
        Emotion.SAD -> "sad 😢"
        Emotion.LONELY -> "lonely 😔"
    }
