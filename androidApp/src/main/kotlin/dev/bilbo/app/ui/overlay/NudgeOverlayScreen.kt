package dev.bilbo.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
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

// ── Nudge palette: warm amber tones ──────────────────────────────────────────
private const val ARGB_BACKGROUND = 0x80000000
private const val ARGB_CARD = 0xFF2C1F0E
private const val ARGB_AMBER = 0xFFF5A623
private const val ARGB_AMBER_SOFT = 0xFFFFD280
private const val ARGB_ON_SURFACE = 0xFFF5ECD7
private const val ARGB_SUBTLE = 0xFFAA8C6A
private const val ARGB_GREEN = 0xFF6BCB77

private val NudgeBackground = Color(ARGB_BACKGROUND) // translucent scrim
private val NudgeCard = Color(ARGB_CARD) // deep warm brown
private val NudgeAmber = Color(ARGB_AMBER) // primary amber
private val NudgeAmberSoft = Color(ARGB_AMBER_SOFT) // lighter amber text
private val NudgeOnSurface = Color(ARGB_ON_SURFACE) // warm off-white
private val NudgeSubtle = Color(ARGB_SUBTLE) // muted warm text
private val NudgeGreen = Color(ARGB_GREEN) // "Got it" positive action

private const val EXTEND_COST_FP = 5
private const val FADE_IN_MS = 300
private const val SLIDE_IN_MS = 380
private const val EMOJI_SP = 40
private const val TITLE_LINE_HEIGHT_SP = 26
private const val OUTER_PAD_H_DP = 16
private const val OUTER_PAD_V_DP = 24
private const val CARD_CORNER_DP = 20
private const val CARD_ELEVATION_DP = 16
private const val CARD_PAD_H_DP = 24
private const val CARD_PAD_V_DP = 28
private const val BUTTON_HEIGHT_DP = 52
private const val EXTEND_BUTTON_HEIGHT_DP = 48
private const val BUTTON_CORNER_DP = 14
private const val BORDER_THIN_DP = 1
private const val ALPHA_EXTEND_BORDER = 0.5f
private const val SPACE_TITLE_DP = 14
private const val SPACE_USAGE_DP = 10
private const val SPACE_ACTIONS_DP = 28
private const val SPACE_BETWEEN_DP = 10
private const val SPACE_BOTTOM_DP = 4

/**
 * Translucent top-card nudge overlay that appears when a declared session timer expires
 * under [EnforcementMode.NUDGE].
 *
 * @param appName          Human-readable name of the app.
 * @param declaredMinutes  The duration the user committed to at intent declaration time.
 * @param actualMinutes    How long they actually spent (may exceed declared).
 * @param fpBalance        Current Focus Points balance (for 5-min extension cost display).
 * @param onGotIt          User acknowledges and dismisses the nudge.
 * @param onExtend5Min     User requests a 5-minute extension (costs 5 FP).
 *                         Only shown when [fpBalance] >= 5.
 */
@Composable
fun NudgeOverlayScreen(
    appName: String,
    declaredMinutes: Int,
    actualMinutes: Int,
    fpBalance: Int,
    onGotIt: () -> Unit,
    onExtend5Min: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val canExtend = fpBalance >= EXTEND_COST_FP

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(NudgeBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(tween(FADE_IN_MS)) +
                    slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(SLIDE_IN_MS, easing = EaseOutCubic),
                    ),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = OUTER_PAD_H_DP.dp, vertical = OUTER_PAD_V_DP.dp),
                shape = RoundedCornerShape(CARD_CORNER_DP.dp),
                colors = CardDefaults.cardColors(containerColor = NudgeCard),
                elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CARD_PAD_H_DP.dp, vertical = CARD_PAD_V_DP.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "⏰", fontSize = EMOJI_SP.sp)
                    Spacer(Modifier.height(SPACE_TITLE_DP.dp))
                    NudgeTitle(appName = appName, declaredMinutes = declaredMinutes)
                    Spacer(Modifier.height(SPACE_USAGE_DP.dp))
                    Text(
                        text = "You've been here for $actualMinutes minute${if (actualMinutes != 1) "s" else ""}.",
                        style =
                            androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                color = NudgeSubtle,
                            ),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(SPACE_ACTIONS_DP.dp))
                    GotItButton(onGotIt = onGotIt)
                    Spacer(Modifier.height(SPACE_BETWEEN_DP.dp))
                    ExtensionAction(canExtend = canExtend, fpBalance = fpBalance, onExtend5Min = onExtend5Min)
                    Spacer(Modifier.height(SPACE_BOTTOM_DP.dp))
                }
            }
        }
    }
}

@Composable
private fun NudgeTitle(
    appName: String,
    declaredMinutes: Int,
) {
    Text(
        text =
            buildAnnotatedString {
                append("Time's up! Your ")
                withStyle(SpanStyle(color = NudgeAmber, fontWeight = FontWeight.Bold)) {
                    append("$declaredMinutes min")
                }
                append(" on ")
                withStyle(SpanStyle(color = NudgeAmberSoft, fontWeight = FontWeight.SemiBold)) {
                    append(appName)
                }
                append(" is over.")
            },
        style =
            androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                color = NudgeOnSurface,
                lineHeight = TITLE_LINE_HEIGHT_SP.sp,
            ),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GotItButton(onGotIt: () -> Unit) {
    Button(
        onClick = onGotIt,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NudgeGreen),
    ) {
        Text(
            text = "Got it",
            style =
                androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
        )
    }
}

@Composable
private fun ExtensionAction(
    canExtend: Boolean,
    fpBalance: Int,
    onExtend5Min: () -> Unit,
) {
    if (canExtend) {
        OutlinedButton(
            onClick = onExtend5Min,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(EXTEND_BUTTON_HEIGHT_DP.dp),
            shape = RoundedCornerShape(BUTTON_CORNER_DP.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NudgeAmber),
            border =
                androidx.compose.foundation.BorderStroke(
                    width = BORDER_THIN_DP.dp,
                    color = NudgeAmber.copy(alpha = ALPHA_EXTEND_BORDER),
                ),
        ) {
            Text(
                text = "$EXTEND_COST_FP more minutes  (−$EXTEND_COST_FP FP)",
                style =
                    androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = NudgeAmber,
                    ),
            )
        }
    } else {
        Text(
            text = "Balance: $fpBalance FP · Not enough to extend",
            style =
                androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    color = NudgeSubtle,
                ),
            textAlign = TextAlign.Center,
        )
    }
}
