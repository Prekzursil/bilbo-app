package dev.bilbo.app.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Hard lock palette: deep blue / purple calming ─────────────────────────────
private const val ARGB_GRADIENT_TOP = 0xFF0D1B3E
private const val ARGB_GRADIENT_BOT = 0xFF1A0D33
private const val ARGB_PURPLE = 0xFF7B61FF
private const val ARGB_PURPLE_DIM = 0xFF4A3B99
private const val ARGB_ON_SURFACE = 0xFFDDE3F5
private const val ARGB_SUBTLE = 0xFF8892B0
private const val ARGB_SURFACE = 0xFF131B34
private const val ARGB_RED = 0xFFFF6B6B
private const val ARGB_GREEN = 0xFF64D9A8
private const val ARGB_GO_HOME_LABEL = 0xFF001A0D

private val LockGradientTop = Color(ARGB_GRADIENT_TOP) // deep navy
private val LockGradientBot = Color(ARGB_GRADIENT_BOT) // deep purple
private val LockPurple = Color(ARGB_PURPLE) // soft purple accent
private val LockPurpleDim = Color(ARGB_PURPLE_DIM) // dimmer purple
private val LockOnSurface = Color(ARGB_ON_SURFACE) // cool lavender-white
private val LockSubtle = Color(ARGB_SUBTLE) // muted blue-grey
private val LockSurface = Color(ARGB_SURFACE) // card surface
private val LockRed = Color(ARGB_RED) // override warning
private val LockGreen = Color(ARGB_GREEN) // home button

// ── Override economics ────────────────────────────────────────────────────────
private const val OVERRIDE_COST_FP = 10

// ── Layout / animation tuning ─────────────────────────────────────────────────
private const val PULSE_MIN = 0.95f
private const val PULSE_MAX = 1.05f
private const val PULSE_PERIOD_MS = 2_000
private const val SCREEN_PADDING_DP = 28
private const val ICON_SIZE_DP = 80
private const val ICON_FONT_SP = 36
private const val ALPHA_ICON_FILL = 0.15f
private const val ALPHA_ICON_BORDER = 0.4f
private const val ALPHA_CARD_BORDER = 0.2f
private const val ALPHA_COUNTDOWN_FILL = 0.25f
private const val ALPHA_COUNTDOWN_BORDER = 0.3f
private const val ALPHA_OVERRIDE_LABEL = 0.8f
private const val BORDER_THIN_DP = 1
private const val HEADER_LINE_HEIGHT_SP = 32
private const val CARD_CORNER_DP = 16
private const val CARD_PADDING_DP = 18
private const val LABEL_LETTER_SPACING_SP = 0.8
private const val COUNTDOWN_CORNER_DP = 20
private const val COUNTDOWN_PAD_H_DP = 32
private const val COUNTDOWN_PAD_V_DP = 16
private const val COUNTDOWN_LETTER_SPACING_SP = 4
private const val COUNTDOWN_FONT_SP = 52
private const val BUTTON_HEIGHT_DP = 56
private const val SPACE_XS_DP = 6
private const val SPACE_SM_DP = 8
private const val SPACE_MD_DP = 12
private const val SPACE_LG_DP = 24
private const val SPACE_XL_DP = 32
private const val SPACE_XXL_DP = 40

// ── Time conversion ───────────────────────────────────────────────────────────
private const val SECONDS_PER_HOUR = 3_600
private const val SECONDS_PER_MINUTE = 60

/**
 * Immutable view-state for the [HardLockOverlayScreen], grouped to keep the
 * composable's parameter list small and self-documenting.
 *
 * @property appName          Human-readable app name.
 * @property cooldownMinutes  Total cooldown duration in minutes.
 * @property remainingSeconds Remaining cooldown in seconds — drives the countdown display.
 * @property suggestion       Analog alternative suggestion text.
 * @property fpBalance        Current Focus Points balance.
 */
data class HardLockUiState(
    val appName: String,
    val cooldownMinutes: Int,
    val remainingSeconds: Long,
    val suggestion: String,
    val fpBalance: Int,
)

/**
 * Full-screen, opaque Hard Lock overlay shown when [EnforcementMode.HARD_LOCK] fires.
 *
 * The user can:
 * - Go home (safe action)
 * - Override (costs [OVERRIDE_COST_FP] FP, requires confirmation, disabled if insufficient balance)
 *
 * @param state     Immutable view-state (app name, cooldown, balance, suggestion).
 * @param onGoHome  User taps "Go Home".
 * @param onOverride User confirms the override (FP deducted by caller).
 */
@Composable
fun HardLockOverlayScreen(
    state: HardLockUiState,
    onGoHome: () -> Unit,
    onOverride: () -> Unit,
) {
    var showOverrideDialog by remember { mutableStateOf(false) }
    val canOverride = state.fpBalance >= OVERRIDE_COST_FP

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(LockGradientTop, LockGradientBot)),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = SCREEN_PADDING_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulsingLockIcon()

            Spacer(Modifier.height(SPACE_LG_DP.dp))

            LockHeader(appName = state.appName, cooldownMinutes = state.cooldownMinutes)

            Spacer(Modifier.height(SPACE_XL_DP.dp))

            CooldownCounterDisplay(remainingSeconds = state.remainingSeconds)

            Spacer(Modifier.height(SPACE_XL_DP.dp))

            SuggestionCard(suggestion = state.suggestion)

            Spacer(Modifier.height(SPACE_XXL_DP.dp))

            GoHomeButton(onGoHome = onGoHome)

            Spacer(Modifier.height(SPACE_MD_DP.dp))

            OverrideButton(canOverride = canOverride, onClick = { showOverrideDialog = true })

            Spacer(Modifier.height(SPACE_SM_DP.dp))

            Text(
                text = "Balance: ${state.fpBalance} FP",
                style = MaterialTheme.typography.labelSmall.copy(color = LockSubtle),
            )
        }
    }

    if (showOverrideDialog) {
        OverrideDialog(
            canOverride = canOverride,
            fpBalance = state.fpBalance,
            onConfirm = {
                showOverrideDialog = false
                onOverride()
            },
            onDismiss = { showOverrideDialog = false },
        )
    }
}

@Composable
private fun PulsingLockIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "LockPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = PULSE_MIN,
        targetValue = PULSE_MAX,
        animationSpec =
            infiniteRepeatable(
                animation = tween(PULSE_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "PulseScale",
    )
    Box(
        modifier =
            Modifier
                .scale(pulse)
                .size(ICON_SIZE_DP.dp)
                .clip(CircleShape)
                .background(LockPurple.copy(alpha = ALPHA_ICON_FILL))
                .border(BORDER_THIN_DP.dp, LockPurple.copy(alpha = ALPHA_ICON_BORDER), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "🔒", fontSize = ICON_FONT_SP.sp)
    }
}

@Composable
private fun LockHeader(
    appName: String,
    cooldownMinutes: Int,
) {
    Text(
        text =
            buildAnnotatedString {
                append("Time's up. ")
                withStyle(SpanStyle(color = LockPurple, fontWeight = FontWeight.Bold)) {
                    append(appName)
                }
                append(" is locked for $cooldownMinutes minutes.")
            },
        style =
            MaterialTheme.typography.headlineSmall.copy(
                color = LockOnSurface,
                fontWeight = FontWeight.SemiBold,
                lineHeight = HEADER_LINE_HEIGHT_SP.sp,
            ),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SuggestionCard(suggestion: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = LockSurface),
        border =
            androidx.compose.foundation.BorderStroke(
                BORDER_THIN_DP.dp,
                LockPurple.copy(alpha = ALPHA_CARD_BORDER),
            ),
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "While you wait, try:",
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        color = LockSubtle,
                        letterSpacing = LABEL_LETTER_SPACING_SP.sp,
                    ),
            )
            Spacer(Modifier.height(SPACE_XS_DP.dp))
            Text(
                text = suggestion,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = LockOnSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GoHomeButton(onGoHome: () -> Unit) {
    Button(
        onClick = onGoHome,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LockGreen),
    ) {
        Text(
            text = "Go Home",
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(ARGB_GO_HOME_LABEL),
                ),
        )
    }
}

@Composable
private fun OverrideButton(
    canOverride: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (canOverride) "Override (costs $OVERRIDE_COST_FP FP)" else "Override (not enough FP)",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = if (canOverride) LockRed.copy(alpha = ALPHA_OVERRIDE_LABEL) else LockSubtle,
                ),
        )
    }
}

@Composable
private fun OverrideDialog(
    canOverride: Boolean,
    fpBalance: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockSurface,
        title = {
            Text(
                text = "Override Lock?",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        color = LockOnSurface,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        },
        text = {
            val body =
                if (canOverride) {
                    "Override costs $OVERRIDE_COST_FP Focus Points.\n\nCurrent balance: $fpBalance FP.\n\nContinue?"
                } else {
                    "Not enough Focus Points to override.\n\n" +
                        "You need $OVERRIDE_COST_FP FP but only have $fpBalance FP."
                }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(color = LockSubtle),
            )
        },
        confirmButton = {
            if (canOverride) {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "Yes, override",
                        style = MaterialTheme.typography.labelLarge.copy(color = LockRed),
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(color = LockPurple),
                )
            }
        },
    )
}

// ── Countdown display component ───────────────────────────────────────────────

@Composable
private fun CooldownCounterDisplay(remainingSeconds: Long) {
    val hours = remainingSeconds / SECONDS_PER_HOUR
    val minutes = (remainingSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = remainingSeconds % SECONDS_PER_MINUTE

    val timeStr =
        if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }

    Box(
        modifier =
            Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(COUNTDOWN_CORNER_DP.dp))
                .background(LockPurpleDim.copy(alpha = ALPHA_COUNTDOWN_FILL))
                .border(
                    BORDER_THIN_DP.dp,
                    LockPurple.copy(alpha = ALPHA_COUNTDOWN_BORDER),
                    RoundedCornerShape(COUNTDOWN_CORNER_DP.dp),
                )
                .padding(horizontal = COUNTDOWN_PAD_H_DP.dp, vertical = COUNTDOWN_PAD_V_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = timeStr,
            style =
                MaterialTheme.typography.displayMedium.copy(
                    color = LockOnSurface,
                    fontWeight = FontWeight.Light,
                    letterSpacing = COUNTDOWN_LETTER_SPACING_SP.sp,
                    fontSize = COUNTDOWN_FONT_SP.sp,
                ),
        )
    }
}
