package dev.bilbo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.domain.FPEconomy

private const val ARGB_FP_GREEN = 0xFF4CAF50
private const val ARGB_FP_YELLOW = 0xFFFFC107
private const val ARGB_FP_RED = 0xFFE53935

// ── Semantic colour thresholds ─────────────────────────────────────────────────
private val FpGreen = Color(ARGB_FP_GREEN)
private val FpYellow = Color(ARGB_FP_YELLOW)
private val FpRed = Color(ARGB_FP_RED)

private const val HIGH_THRESHOLD = 30
private const val LOW_THRESHOLD = 10
private const val DAILY_CAP = FPEconomy.DAILY_EARN_CAP.toFloat() // 60

private const val DEFAULT_SIZE_DP = 120
private const val COLOR_ANIM_MS = 600
private const val PROGRESS_ANIM_MS = 800
private const val TRACK_ALPHA = 0.18f
private const val STROKE_FRACTION = 0.07f
private const val ARC_START_ANGLE = -90f
private const val ARC_FULL_SWEEP = 360f
private const val BALANCE_FONT_FRACTION = 0.25f
private const val LABEL_FONT_FRACTION = 0.1f

/**
 * Reusable Focus Points balance widget.
 *
 * Displays:
 * - A circular progress ring showing earned FP vs the daily cap (60).
 * - The current balance as a large number, colour-coded by amount.
 * - A "Focus Points" label beneath the number.
 *
 * @param currentBalance  The live FP balance (may be negative).
 * @param fpEarned        FP earned today (used for the ring fill fraction).
 * @param modifier        Optional external modifier.
 * @param size            Diameter of the outer circle.  Defaults to 120 dp.
 */
@Composable
fun FPBalanceWidget(
    currentBalance: Int,
    fpEarned: Int,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_SIZE_DP.dp,
) {
    // Colour transitions smoothly when the balance crosses thresholds.
    val balanceColor by animateColorAsState(
        targetValue =
            when {
                currentBalance > HIGH_THRESHOLD -> FpGreen
                currentBalance > LOW_THRESHOLD -> FpYellow
                else -> FpRed
            },
        animationSpec = tween(durationMillis = COLOR_ANIM_MS),
        label = "FpColor",
    )

    // Ring fill: clamp earned FP to [0, cap].
    val progressFraction by animateFloatAsState(
        targetValue = (fpEarned.coerceIn(0, DAILY_CAP.toInt()) / DAILY_CAP),
        animationSpec = tween(durationMillis = PROGRESS_ANIM_MS),
        label = "FpProgress",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        ProgressRing(size = size, balanceColor = balanceColor, progressFraction = progressFraction)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = currentBalance.toString(),
                style =
                    MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        fontSize = (size.value * BALANCE_FONT_FRACTION).sp,
                    ),
            )
            Text(
                text = "Focus Points",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (size.value * LABEL_FONT_FRACTION).sp,
                    ),
            )
        }
    }
}

@Composable
private fun ProgressRing(
    size: Dp,
    balanceColor: Color,
    progressFraction: Float,
) {
    val trackColor = balanceColor.copy(alpha = TRACK_ALPHA)
    val strokeWidth = (size.value * STROKE_FRACTION).dp

    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = ARC_START_ANGLE,
            sweepAngle = ARC_FULL_SWEEP,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = balanceColor,
            startAngle = ARC_START_ANGLE,
            sweepAngle = progressFraction * ARC_FULL_SWEEP,
            useCenter = false,
            style = stroke,
        )
    }
}
