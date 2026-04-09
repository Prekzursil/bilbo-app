package dev.spark.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.spark.app.ui.components.FPBalanceWidget
import dev.spark.app.ui.theme.SparkTheme

// ── Colours reused from the gatekeeper palette ────────────────────────────────
private val GkPrimary   = Color(0xFF48B8A0)
private val FpGreen     = Color(0xFF4CAF50)
private val FpYellow    = Color(0xFFFFC107)
private val FpRed       = Color(0xFFE53935)
private val GkOnSurface = Color(0xFFE0EEF5)
private val GkSurface   = Color(0xFF243344)
private val GkSubtle    = Color(0xFF8AAFC4)

/**
 * Focus Points section shown inside [GatekeeperScreen] for Empty Calorie apps.
 *
 * Communicates:
 * - The user's current FP balance (via [FPBalanceWidget]).
 * - The approximate FP cost for the selected duration.
 * - A warning if balance is zero or low.
 *
 * The **Start** button is expected to be disabled externally when [currentBalance] ≤ 0.
 *
 * @param currentBalance     Live FP balance.
 * @param fpEarned           FP earned today (used by the ring indicator).
 * @param estimatedCostFp    Estimated FP cost = selected session duration in minutes × 1 FP/min.
 * @param isEmptyCalorieApp  Pass `false` for Nutritive/Neutral apps (section is hidden).
 */
@Composable
fun GatekeeperFPSection(
    currentBalance: Int,
    fpEarned: Int,
    estimatedCostFp: Int,
    isEmptyCalorieApp: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isEmptyCalorieApp,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GkSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // ── Balance + ring ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FPBalanceWidget(
                    currentBalance = currentBalance,
                    fpEarned = fpEarned,
                    size = 72.dp,
                )

                Column {
                    Text(
                        text = "Your Focus Points",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = GkSubtle,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "This will cost ~$estimatedCostFp FP",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GkOnSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            // ── Status message ────────────────────────────────────────────
            when {
                currentBalance <= 0 -> FpStatusBanner(
                    message = "No Focus Points remaining. Earn more first.",
                    color = FpRed,
                )
                currentBalance < 10 -> FpStatusBanner(
                    message = "Low balance. $currentBalance FP remaining.",
                    color = FpYellow,
                )
                else -> {}   // All good — no banner needed
            }
        }
    }
}

// ── Status banner ─────────────────────────────────────────────────────────────

@Composable
private fun FpStatusBanner(message: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
            )
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = color),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF1A2C3D)
@Composable
private fun FPSectionEmptyPreview() {
    SparkTheme {
        GatekeeperFPSection(
            currentBalance = 0,
            fpEarned = 5,
            estimatedCostFp = 15,
            isEmptyCalorieApp = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A2C3D)
@Composable
private fun FPSectionLowPreview() {
    SparkTheme {
        GatekeeperFPSection(
            currentBalance = 7,
            fpEarned = 12,
            estimatedCostFp = 10,
            isEmptyCalorieApp = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A2C3D)
@Composable
private fun FPSectionOkPreview() {
    SparkTheme {
        GatekeeperFPSection(
            currentBalance = 42,
            fpEarned = 30,
            estimatedCostFp = 15,
            isEmptyCalorieApp = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}
