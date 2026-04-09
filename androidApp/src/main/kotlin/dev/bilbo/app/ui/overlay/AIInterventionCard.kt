package dev.bilbo.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
private val AiBackground  = Color(0x80000000)    // scrim
private val AiCard        = Color(0xFF1E1528)    // deep warm violet-grey
private val AiPurple      = Color(0xFFB08DFF)    // soft lilac accent
private val AiPurpleDim   = Color(0xFF7C5CBF)    // dim purple
private val AiOnSurface   = Color(0xFFEDE8F8)    // lavender-white
private val AiSubtle      = Color(0xFF998AB8)    // muted lavender
private val AiSurface     = Color(0xFF2A1E3C)    // slightly lighter card
private val AiGreen       = Color(0xFF6BCB77)    // breathe button
private val AiNeutral     = Color(0xFF5A5A6A)    // continue button outline

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
        modifier = Modifier
            .fillMaxSize()
            .background(AiBackground),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(280)) + slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(360),
            ),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AiCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    // ── Brain/insight icon ────────────────────────────────────
                    Text(text = "✨", fontSize = 36.sp)

                    Spacer(Modifier.height(16.dp))

                    // ── Pattern observation ───────────────────────────────────
                    PatternObservationText(
                        emotion = emotion,
                        appName = appName,
                        avgDurationMins = avgDurationMins,
                        postMood = postMood,
                    )

                    Spacer(Modifier.height(20.dp))

                    HorizontalDivider(
                        color = AiPurpleDim.copy(alpha = 0.3f),
                        thickness = 1.dp,
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Breathe suggestion ────────────────────────────────────
                    Text(
                        text = "Would you like to try 2 minutes\nof breathing instead?",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = AiOnSurface,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Breathe CTA ───────────────────────────────────────────
                    Button(
                        onClick = onBreathe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AiGreen),
                    ) {
                        Text(
                            text = "Yes, let me breathe 🌬️",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF001A0D),
                            ),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Continue anyway ───────────────────────────────────────
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AiSubtle,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, AiNeutral.copy(alpha = 0.5f)
                        ),
                    ) {
                        Text(
                            text = "Continue to $appName",
                            style = MaterialTheme.typography.bodyMedium.copy(color = AiSubtle),
                        )
                    }
                }
            }
        }
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
        text = buildAnnotatedString {
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
        style = MaterialTheme.typography.bodyMedium.copy(
            color = AiSubtle,
            lineHeight = 22.sp,
        ),
        textAlign = TextAlign.Center,
    )
}

// ── Emotion display helpers ───────────────────────────────────────────────────

private fun Emotion.displayLabel(): String = when (this) {
    Emotion.HAPPY    -> "happy 😊"
    Emotion.CALM     -> "calm 😌"
    Emotion.BORED    -> "bored 😑"
    Emotion.STRESSED -> "stressed 😫"
    Emotion.ANXIOUS  -> "anxious 😰"
    Emotion.SAD      -> "sad 😢"
    Emotion.LONELY   -> "lonely 😔"
}
