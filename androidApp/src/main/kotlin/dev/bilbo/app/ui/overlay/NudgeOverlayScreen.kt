package dev.bilbo.app.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Nudge palette: warm amber tones ──────────────────────────────────────────
private val NudgeBackground  = Color(0x80000000)   // translucent scrim
private val NudgeCard        = Color(0xFF2C1F0E)   // deep warm brown
private val NudgeAmber       = Color(0xFFF5A623)   // primary amber
private val NudgeAmberSoft   = Color(0xFFFFD280)   // lighter amber text
private val NudgeSurface     = Color(0xFF3D2B14)   // slightly lighter card surface
private val NudgeOnSurface   = Color(0xFFF5ECD7)   // warm off-white
private val NudgeSubtle      = Color(0xFFAA8C6A)   // muted warm text
private val NudgeGreen       = Color(0xFF6BCB77)   // "Got it" positive action

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

    val canExtend = fpBalance >= 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NudgeBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(380, easing = EaseOutCubic),
            ),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NudgeCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ── Emoji header ─────────────────────────────────────────
                    Text(
                        text = "⏰",
                        fontSize = 40.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    // ── Title ────────────────────────────────────────────────
                    Text(
                        text = buildAnnotatedString {
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
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            color = NudgeOnSurface,
                            lineHeight = 26.sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))

                    // ── Actual usage ─────────────────────────────────────────
                    Text(
                        text = "You've been here for $actualMinutes minute${if (actualMinutes != 1) "s" else ""}.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            color = NudgeSubtle,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Got it button ────────────────────────────────────────
                    Button(
                        onClick = onGotIt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NudgeGreen),
                    ) {
                        Text(
                            text = "Got it",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            ),
                        )
                    }

                    // ── Extension button (conditional) ────────────────────────
                    if (canExtend) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onExtend5Min,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NudgeAmber,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = NudgeAmber.copy(alpha = 0.5f),
                            ),
                        ) {
                            Text(
                                text = "5 more minutes  (−5 FP)",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = NudgeAmber,
                                ),
                            )
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Balance: $fpBalance FP · Not enough to extend",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                color = NudgeSubtle,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
