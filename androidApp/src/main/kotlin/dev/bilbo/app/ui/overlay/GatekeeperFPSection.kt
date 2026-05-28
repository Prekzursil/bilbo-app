package dev.bilbo.app.ui.overlay

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
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.components.FPBalanceWidget

private const val LOW_BALANCE_THRESHOLD = 10
private const val WIDGET_SIZE_DP = 72
private const val CONTAINER_RADIUS_DP = 14
private const val BANNER_RADIUS_DP = 8
private const val PADDING_H_DP = 16
private const val PADDING_V_DP = 14
private const val SPACE_MEDIUM_PLUS_DP = 10
private const val SPACE_LARGE_DP = 16
private const val SPACE_SMALL_DP = 8
private const val SPACE_XS_DP = 2
private const val BORDER_WIDTH_DP = 1
private const val BANNER_PADDING_H_DP = 12
private const val ICON_SIZE_DP = 16
private const val ALPHA_BORDER = 0.5f
private const val ALPHA_FILL = 0.1f

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
 * @param modifier           Layout modifier applied to the section container.
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
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CONTAINER_RADIUS_DP.dp))
                    .background(OverlayPalette.Surface)
                    .padding(horizontal = PADDING_H_DP.dp, vertical = PADDING_V_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SPACE_MEDIUM_PLUS_DP.dp),
        ) {
            // ── Balance + ring ────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SPACE_LARGE_DP.dp),
            ) {
                FPBalanceWidget(
                    currentBalance = currentBalance,
                    fpEarned = fpEarned,
                    size = WIDGET_SIZE_DP.dp,
                )

                Column {
                    Text(
                        text = "Your Focus Points",
                        style = MaterialTheme.typography.labelMedium.copy(color = OverlayPalette.Subtle),
                    )
                    Spacer(modifier = Modifier.height(SPACE_XS_DP.dp))
                    Text(
                        text = "This will cost ~$estimatedCostFp FP",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = OverlayPalette.OnSurface,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }
            }

            // ── Status message ────────────────────────────────────────────
            when {
                currentBalance <= 0 ->
                    FpStatusBanner(
                        message = "No Focus Points remaining. Earn more first.",
                        color = OverlayPalette.FpRed,
                    )
                currentBalance < LOW_BALANCE_THRESHOLD ->
                    FpStatusBanner(
                        message = "Low balance. $currentBalance FP remaining.",
                        color = OverlayPalette.FpYellow,
                    )
                else -> {} // All good — no banner needed
            }
        }
    }
}

// ── Status banner ─────────────────────────────────────────────────────────────

@Composable
private fun FpStatusBanner(
    message: String,
    color: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BANNER_RADIUS_DP.dp))
                .border(
                    width = BORDER_WIDTH_DP.dp,
                    color = color.copy(alpha = ALPHA_BORDER),
                    shape = RoundedCornerShape(BANNER_RADIUS_DP.dp),
                ).background(color.copy(alpha = ALPHA_FILL))
                .padding(horizontal = BANNER_PADDING_H_DP.dp, vertical = SPACE_SMALL_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACE_SMALL_DP.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(ICON_SIZE_DP.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = color),
        )
    }
}
