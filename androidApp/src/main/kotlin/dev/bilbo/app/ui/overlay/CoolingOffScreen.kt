package dev.bilbo.app.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ARGB_BG_TOP = 0xFF0A1A24
private const val ARGB_BG_BOT = 0xFF0D2133
private const val ARGB_CIRCLE_IN = 0xFF48B8A0
private const val ARGB_CIRCLE_OUT = 0xFF6EA8C8
private const val ARGB_GLOW = 0x3048B8A0
private const val ARGB_ON_SURFACE = 0xFFD5EDF5
private const val ARGB_SUBTLE = 0xFF7FB4CA
private const val ARGB_PROGRESS = 0xFF48B8A0
private const val ARGB_PROGRESS_BG = 0xFF1A3344

// ── Cooling-off palette: soft teal to light blue ──────────────────────────────
private val CoBgTop = Color(ARGB_BG_TOP) // deep teal-black
private val CoBgBot = Color(ARGB_BG_BOT) // dark ocean
private val CoCircleIn = Color(ARGB_CIRCLE_IN) // inhale teal
private val CoCircleOut = Color(ARGB_CIRCLE_OUT) // exhale blue
private val CoGlow = Color(ARGB_GLOW) // circle glow
private val CoOnSurface = Color(ARGB_ON_SURFACE) // cool white
private val CoSubtle = Color(ARGB_SUBTLE) // muted text
private val CoProgress = Color(ARGB_PROGRESS) // progress bar
private val CoProgressBg = Color(ARGB_PROGRESS_BG) // progress bg

// Breathing cycle: 4s in, 4s out, 2s hold = 10s total
private const val TOTAL_DURATION_SECS = 10
private const val INHALE_DURATION_MS = 4_000
private const val EXHALE_DURATION_MS = 4_000
private const val HOLD_DURATION_MS = 2_000
private const val TICK_MS = 1_000L

private const val SCALE_MIN = 0.4f
private const val SCALE_MAX = 1.0f
private const val CIRCLE_SIZE_DP = 220
private const val GLOW_RADIUS_FACTOR = 1.25f
private const val MID_RING_FACTOR = 1.1f
private const val HOLD_LERP = 0.5f
private const val ALPHA_MID_RING = 0.15f
private const val ALPHA_BORDER = 0.6f
private const val ALPHA_FILL_INNER = 0.55f
private const val ALPHA_FILL_MID = 0.20f
private const val ALPHA_FILL_OUTER = 0.05f
private const val ALPHA_FP_LABEL = 0.7f
private const val BORDER_STROKE_DP = 2

private const val SCREEN_PADDING_DP = 32
private const val SPACE_TOP_DP = 64
private const val SPACE_SECTION_DP = 48
private const val SPACE_MEDIUM_DP = 24
private const val SPACE_SMALL_DP = 6
private const val PROGRESS_HEIGHT_DP = 4
private const val PROGRESS_RADIUS_DP = 2
private const val LETTER_SPACING_SP = 1
private const val COUNTDOWN_FONT_SP = 48

/**
 * Full-screen 10-second breathing animation screen.
 *
 * - Circle expands over 4 seconds (inhale), contracts over 4 seconds (exhale),
 *   holds for 2 seconds.
 * - Cannot be dismissed — hard 10 seconds with back navigation disabled.
 * - Awards +3 FP on completion then calls [onComplete].
 *
 * @param onComplete Called after the 10-second cycle completes (should award FP and open app).
 */
@Composable
fun CoolingOffScreen(onComplete: () -> Unit) {
    // Circular radius scale: 0f = minimum, 1f = maximum
    val circleScale = remember { Animatable(SCALE_MIN) }
    var instruction by remember { mutableStateOf("Breathe in...") }
    var remainingSecs by remember { mutableIntStateOf(TOTAL_DURATION_SECS) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }

    // Run the breathing animation sequence — cannot be cancelled by user
    LaunchedEffect(Unit) {
        val countdownJob =
            launch {
                repeat(TOTAL_DURATION_SECS) {
                    delay(TICK_MS)
                    remainingSecs = (TOTAL_DURATION_SECS - it - 1).coerceAtLeast(0)
                }
            }

        phase = BreathPhase.INHALE
        instruction = "Breathe in..."
        circleScale.animateTo(SCALE_MAX, tween(durationMillis = INHALE_DURATION_MS, easing = EaseInOut))

        phase = BreathPhase.EXHALE
        instruction = "Breathe out..."
        circleScale.animateTo(SCALE_MIN, tween(durationMillis = EXHALE_DURATION_MS, easing = EaseInOut))

        phase = BreathPhase.HOLD
        instruction = "Hold..."
        delay(HOLD_DURATION_MS.toLong())

        countdownJob.cancel()
        remainingSecs = 0
        onComplete()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CoBgTop, CoBgBot))),
        contentAlignment = Alignment.Center,
    ) {
        CoolingOffContent(
            instruction = instruction,
            remainingSecs = remainingSecs,
            circleScale = circleScale.value,
            phase = phase,
        )
    }
}

@Composable
private fun CoolingOffContent(
    instruction: String,
    remainingSecs: Int,
    circleScale: Float,
    phase: BreathPhase,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SCREEN_PADDING_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(SPACE_TOP_DP.dp))
        Text(
            text = instruction,
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    color = CoOnSurface,
                    fontWeight = FontWeight.Light,
                    letterSpacing = LETTER_SPACING_SP.sp,
                ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SPACE_SECTION_DP.dp))
        BreathingCircle(scale = circleScale, phase = phase)
        Spacer(Modifier.height(SPACE_SECTION_DP.dp))
        Text(
            text = "$remainingSecs",
            style =
                MaterialTheme.typography.displaySmall.copy(
                    color = CoSubtle,
                    fontWeight = FontWeight.Thin,
                    fontSize = COUNTDOWN_FONT_SP.sp,
                ),
        )
        Spacer(Modifier.height(SPACE_SMALL_DP.dp))
        Text(
            text = "seconds",
            style = MaterialTheme.typography.bodyMedium.copy(color = CoSubtle),
        )
        Spacer(Modifier.height(SCREEN_PADDING_DP.dp))
        LinearProgressIndicator(
            progress = { 1f - (remainingSecs.toFloat() / TOTAL_DURATION_SECS.toFloat()) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PROGRESS_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(PROGRESS_RADIUS_DP.dp)),
            color = CoProgress,
            trackColor = CoProgressBg,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(SPACE_MEDIUM_DP.dp))
        Text(
            text = "+3 FP on completion",
            style = MaterialTheme.typography.labelMedium.copy(color = CoSubtle.copy(alpha = ALPHA_FP_LABEL)),
        )
        Spacer(Modifier.height(SPACE_TOP_DP.dp))
    }
}

// ── Breathing circle Canvas ────────────────────────────────────────────────────

private enum class BreathPhase { INHALE, EXHALE, HOLD }

@Composable
private fun BreathingCircle(
    scale: Float,
    phase: BreathPhase,
) {
    val circleColor =
        when (phase) {
            BreathPhase.INHALE -> CoCircleIn
            BreathPhase.EXHALE -> CoCircleOut
            BreathPhase.HOLD -> lerp(CoCircleIn, CoCircleOut, HOLD_LERP)
        }

    Canvas(
        modifier = Modifier.size(CIRCLE_SIZE_DP.dp),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val currentRadius = maxRadius * scale

        // Outer glow ring
        drawCircle(
            color = CoGlow,
            radius = currentRadius * GLOW_RADIUS_FACTOR,
            center = center,
        )

        // Mid ring
        drawCircle(
            color = circleColor.copy(alpha = ALPHA_MID_RING),
            radius = currentRadius * MID_RING_FACTOR,
            center = center,
        )

        // Main filled circle with radial gradient
        drawBreathCircle(center, currentRadius, circleColor)

        // Stroke border
        drawCircle(
            color = circleColor.copy(alpha = ALPHA_BORDER),
            radius = currentRadius,
            center = center,
            style = Stroke(width = BORDER_STROKE_DP.dp.toPx()),
        )
    }
}

private fun DrawScope.drawBreathCircle(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        color.copy(alpha = ALPHA_FILL_INNER),
                        color.copy(alpha = ALPHA_FILL_MID),
                        color.copy(alpha = ALPHA_FILL_OUTER),
                    ),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
    )
}
