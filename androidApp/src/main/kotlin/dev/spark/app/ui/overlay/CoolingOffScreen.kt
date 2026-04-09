package dev.spark.app.ui.overlay

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Cooling-off palette: soft teal to light blue ──────────────────────────────
private val CoBgTop     = Color(0xFF0A1A24)    // deep teal-black
private val CoBgBot     = Color(0xFF0D2133)    // dark ocean
private val CoCircleIn  = Color(0xFF48B8A0)    // inhale teal
private val CoCircleOut = Color(0xFF6EA8C8)    // exhale blue
private val CoGlow      = Color(0x3048B8A0)    // circle glow
private val CoOnSurface = Color(0xFFD5EDF5)    // cool white
private val CoSubtle    = Color(0xFF7FB4CA)    // muted text
private val CoProgress  = Color(0xFF48B8A0)    // progress bar
private val CoProgressBg= Color(0xFF1A3344)    // progress bg

// Breathing cycle: 4s in, 4s out, 2s hold = 10s total
private const val TOTAL_DURATION_SECS = 10
private const val INHALE_DURATION_MS  = 4_000
private const val EXHALE_DURATION_MS  = 4_000
private const val HOLD_DURATION_MS    = 2_000

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
fun CoolingOffScreen(
    onComplete: () -> Unit,
) {
    // Circular radius scale: 0f = minimum, 1f = maximum
    val circleScale = remember { Animatable(0.4f) }
    var instruction by remember { mutableStateOf("Breathe in...") }
    var remainingSecs by remember { mutableIntStateOf(TOTAL_DURATION_SECS) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }

    // Run the breathing animation sequence — cannot be cancelled by user
    LaunchedEffect(Unit) {
        // Countdown ticker
        val countdownJob = launch {
            repeat(TOTAL_DURATION_SECS) {
                delay(1_000L)
                remainingSecs = (TOTAL_DURATION_SECS - it - 1).coerceAtLeast(0)
            }
        }

        // Inhale: scale 0.4 → 1.0 over 4 seconds
        phase = BreathPhase.INHALE
        instruction = "Breathe in..."
        circleScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = INHALE_DURATION_MS, easing = EaseInOut),
        )

        // Exhale: scale 1.0 → 0.4 over 4 seconds
        phase = BreathPhase.EXHALE
        instruction = "Breathe out..."
        circleScale.animateTo(
            targetValue = 0.4f,
            animationSpec = tween(durationMillis = EXHALE_DURATION_MS, easing = EaseInOut),
        )

        // Hold: 2 seconds static
        phase = BreathPhase.HOLD
        instruction = "Hold..."
        delay(HOLD_DURATION_MS.toLong())

        countdownJob.cancel()
        remainingSecs = 0
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(CoBgTop, CoBgBot))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(Modifier.height(64.dp))

            // ── Instruction text ──────────────────────────────────────────────
            Text(
                text = instruction,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = CoOnSurface,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // ── Breathing circle ──────────────────────────────────────────────
            BreathingCircle(
                scale = circleScale.value,
                phase = phase,
            )

            Spacer(Modifier.height(48.dp))

            // ── Countdown ─────────────────────────────────────────────────────
            Text(
                text = "$remainingSecs",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = CoSubtle,
                    fontWeight = FontWeight.Thin,
                    fontSize = 48.sp,
                ),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "seconds",
                style = MaterialTheme.typography.bodyMedium.copy(color = CoSubtle),
            )

            Spacer(Modifier.height(32.dp))

            // ── Progress bar ──────────────────────────────────────────────────
            val progress = 1f - (remainingSecs.toFloat() / TOTAL_DURATION_SECS.toFloat())
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CoProgress,
                trackColor = CoProgressBg,
                strokeCap = StrokeCap.Round,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "+3 FP on completion",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CoSubtle.copy(alpha = 0.7f),
                ),
            )

            Spacer(Modifier.height(64.dp))
        }
    }
}

// ── Breathing circle Canvas ────────────────────────────────────────────────────

private enum class BreathPhase { INHALE, EXHALE, HOLD }

@Composable
private fun BreathingCircle(scale: Float, phase: BreathPhase) {
    val circleColor = when (phase) {
        BreathPhase.INHALE -> CoCircleIn
        BreathPhase.EXHALE -> CoCircleOut
        BreathPhase.HOLD   -> lerp(CoCircleIn, CoCircleOut, 0.5f)
    }

    Canvas(
        modifier = Modifier.size(220.dp),
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val currentRadius = maxRadius * scale

        // Outer glow ring
        drawCircle(
            color = CoGlow,
            radius = currentRadius * 1.25f,
            center = center,
        )

        // Mid ring
        drawCircle(
            color = circleColor.copy(alpha = 0.15f),
            radius = currentRadius * 1.1f,
            center = center,
        )

        // Main filled circle with radial gradient
        drawBreathCircle(center, currentRadius, circleColor)

        // Stroke border
        drawCircle(
            color = circleColor.copy(alpha = 0.6f),
            radius = currentRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun DrawScope.drawBreathCircle(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.55f),
                color.copy(alpha = 0.20f),
                color.copy(alpha = 0.05f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
