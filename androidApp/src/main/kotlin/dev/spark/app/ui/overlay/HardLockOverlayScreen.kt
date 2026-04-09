package dev.spark.app.ui.overlay

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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.delay

// ── Hard lock palette: deep blue / purple calming ─────────────────────────────
private val LockBackground  = Color(0xFF060B18)    // near-black deep space
private val LockGradientTop = Color(0xFF0D1B3E)    // deep navy
private val LockGradientBot = Color(0xFF1A0D33)    // deep purple
private val LockPurple      = Color(0xFF7B61FF)    // soft purple accent
private val LockPurpleDim   = Color(0xFF4A3B99)    // dimmer purple
private val LockOnSurface   = Color(0xFFDDE3F5)    // cool lavender-white
private val LockSubtle      = Color(0xFF8892B0)    // muted blue-grey
private val LockSurface     = Color(0xFF131B34)    // card surface
private val LockRed         = Color(0xFFFF6B6B)    // override warning
private val LockGreen       = Color(0xFF64D9A8)    // home button

/**
 * Full-screen, opaque Hard Lock overlay shown when [EnforcementMode.HARD_LOCK] fires.
 *
 * The user can:
 * - Go home (safe action)
 * - Override (costs 10 FP, requires confirmation, disabled if insufficient balance)
 *
 * @param appName            Human-readable app name.
 * @param cooldownMinutes    Total cooldown duration in minutes.
 * @param remainingSeconds   Remaining cooldown in seconds — drives the countdown display.
 * @param suggestion         Analog alternative suggestion text.
 * @param fpBalance          Current Focus Points balance.
 * @param onGoHome           User taps "Go Home".
 * @param onOverride         User confirms the override (10 FP deducted by caller).
 */
@Composable
fun HardLockOverlayScreen(
    appName: String,
    cooldownMinutes: Int,
    remainingSeconds: Long,
    suggestion: String,
    fpBalance: Int,
    onGoHome: () -> Unit,
    onOverride: () -> Unit,
) {
    var showOverrideDialog by remember { mutableStateOf(false) }
    val canOverride = fpBalance >= 10

    // Pulsing glow on the lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "LockPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(LockGradientTop, LockGradientBot))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Lock icon ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LockPurple.copy(alpha = 0.15f))
                    .border(1.dp, LockPurple.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🔒", fontSize = 36.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = buildAnnotatedString {
                    append("Time's up. ")
                    withStyle(SpanStyle(color = LockPurple, fontWeight = FontWeight.Bold)) {
                        append(appName)
                    }
                    append(" is locked for $cooldownMinutes minutes.")
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = LockOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 32.sp,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))

            // ── Cooldown countdown ────────────────────────────────────────────
            CooldownCounterDisplay(remainingSeconds = remainingSeconds)

            Spacer(Modifier.height(32.dp))

            // ── Analog suggestion card ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LockSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, LockPurple.copy(alpha = 0.2f)
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "While you wait, try:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = LockSubtle,
                            letterSpacing = 0.8.sp,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = LockOnSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Go Home (primary) ─────────────────────────────────────────────
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LockGreen),
            ) {
                Text(
                    text = "Go Home",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001A0D),
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Override (secondary, destructive) ─────────────────────────────
            TextButton(
                onClick = { showOverrideDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (canOverride) "Override (costs 10 FP)" else "Override (not enough FP)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (canOverride) LockRed.copy(alpha = 0.8f) else LockSubtle,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Balance: $fpBalance FP",
                style = MaterialTheme.typography.labelSmall.copy(color = LockSubtle),
            )
        }
    }

    // ── Override confirmation dialog ──────────────────────────────────────────
    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            containerColor = LockSurface,
            title = {
                Text(
                    text = "Override Lock?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = LockOnSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            text = {
                if (canOverride) {
                    Text(
                        text = "Override costs 10 Focus Points.\n\nCurrent balance: $fpBalance FP.\n\nContinue?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = LockSubtle),
                    )
                } else {
                    Text(
                        text = "Not enough Focus Points to override.\n\nYou need 10 FP but only have $fpBalance FP.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = LockSubtle),
                    )
                }
            },
            confirmButton = {
                if (canOverride) {
                    TextButton(
                        onClick = {
                            showOverrideDialog = false
                            onOverride()
                        },
                    ) {
                        Text(
                            text = "Yes, override",
                            style = MaterialTheme.typography.labelLarge.copy(color = LockRed),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(color = LockPurple),
                    )
                }
            },
        )
    }
}

// ── Countdown display component ───────────────────────────────────────────────

@Composable
private fun CooldownCounterDisplay(remainingSeconds: Long) {
    val hours   = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val timeStr = if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(20.dp))
            .background(LockPurpleDim.copy(alpha = 0.25f))
            .border(1.dp, LockPurple.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.displayMedium.copy(
                color = LockOnSurface,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                fontSize = 52.sp,
            ),
        )
    }
}
