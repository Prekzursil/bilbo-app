package dev.spark.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spark.app.ui.theme.SparkTheme
import dev.spark.domain.FPEconomy

// ── Semantic colour thresholds ─────────────────────────────────────────────────
private val FpGreen  = Color(0xFF4CAF50)
private val FpYellow = Color(0xFFFFC107)
private val FpRed    = Color(0xFFE53935)

private const val HIGH_THRESHOLD   = 30
private const val LOW_THRESHOLD    = 10
private const val DAILY_CAP        = FPEconomy.DAILY_EARN_CAP.toFloat()   // 60

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
 * @param size            Diameter of the outer circle.  Defaults to 120 dp.
 * @param modifier        Optional external modifier.
 */
@Composable
fun FPBalanceWidget(
    currentBalance: Int,
    fpEarned: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    // Colour transitions smoothly when the balance crosses thresholds.
    val balanceColor by animateColorAsState(
        targetValue = when {
            currentBalance > HIGH_THRESHOLD -> FpGreen
            currentBalance > LOW_THRESHOLD  -> FpYellow
            else                            -> FpRed
        },
        animationSpec = tween(durationMillis = 600),
        label = "FpColor",
    )

    // Ring fill: clamp earned FP to [0, cap].
    val progressFraction by animateFloatAsState(
        targetValue = (fpEarned.coerceIn(0, DAILY_CAP.toInt()) / DAILY_CAP),
        animationSpec = tween(durationMillis = 800),
        label = "FpProgress",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // ── Background track ring ──────────────────────────────────────────
        val trackColor = balanceColor.copy(alpha = 0.18f)
        val strokeWidth = (size.value * 0.07f).dp

        androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            // Progress arc
            drawArc(
                color = balanceColor,
                startAngle = -90f,
                sweepAngle = progressFraction * 360f,
                useCenter = false,
                style = stroke,
            )
        }

        // ── Centre content ─────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = currentBalance.toString(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    fontSize = (size.value * 0.25f).sp,
                ),
            )
            Text(
                text = "Focus Points",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (size.value * 0.1f).sp,
                ),
            )
        }
    }
}

// ── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun FPBalanceWidgetHighPreview() {
    SparkTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FPBalanceWidget(currentBalance = 45, fpEarned = 30)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FPBalanceWidgetMidPreview() {
    SparkTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FPBalanceWidget(currentBalance = 18, fpEarned = 20)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FPBalanceWidgetLowPreview() {
    SparkTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FPBalanceWidget(currentBalance = 4, fpEarned = 5, size = 160.dp)
        }
    }
}
