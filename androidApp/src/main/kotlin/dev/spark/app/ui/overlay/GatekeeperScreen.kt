package dev.spark.app.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.spark.app.ui.theme.BilboTheme
import dev.spark.tracking.AppInfo

// ── Brand palette for the gatekeeper overlay ──────────────────────────────────
private val GkBackground = Color(0x99000000)   // scrim
private val GkCard       = Color(0xFF1A2C3D)   // dark teal-navy card
private val GkPrimary    = Color(0xFF48B8A0)   // calming teal
private val GkSurface    = Color(0xFF243344)   // slightly lighter surface
private val GkOnSurface  = Color(0xFFE0EEF5)   // cool white text
private val GkSubtle     = Color(0xFF8AAFC4)   // muted label

private val DURATION_OPTIONS = listOf(5, 10, 15, 20, 30, 60)

/**
 * Full-screen semi-transparent overlay with a card that slides up from the
 * bottom.  Shown when the user opens an app that requires an intent declaration.
 *
 * @param appInfo     The app the user is about to use.
 * @param onStart     Called with [intention] text and [durationMinutes] when
 *                    the user taps "Start".
 * @param onDismiss   Called when the user taps "Not now".
 */
@Composable
fun GatekeeperScreen(
    appInfo: AppInfo,
    onStart: (intention: String, durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var intention by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(15) }

    // Animate the card sliding up from y-offset +100% → 0
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val slideProgress by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(durationMillis = 380),
        label = "GatekeeperSlide",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GkBackground),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = size.height * slideProgress
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = GkCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Drag handle ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GkSubtle.copy(alpha = 0.5f)),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── App icon placeholder + name ───────────────────────────────
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GkPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = appInfo.appLabel.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GkPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = appInfo.appLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GkOnSurface,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Intention prompt ──────────────────────────────────────────
                Text(
                    text = "What's your intention?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GkOnSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = intention,
                    onValueChange = { if (it.length <= 100) intention = it },
                    placeholder = {
                        Text(
                            text = "e.g. check my messages (optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = GkSubtle),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GkPrimary,
                        unfocusedBorderColor = GkSubtle.copy(alpha = 0.4f),
                        focusedTextColor = GkOnSurface,
                        unfocusedTextColor = GkOnSurface,
                        cursorColor = GkPrimary,
                        focusedContainerColor = GkSurface,
                        unfocusedContainerColor = GkSurface,
                    ),
                    supportingText = {
                        Text(
                            text = "${intention.length}/100",
                            style = MaterialTheme.typography.labelSmall.copy(color = GkSubtle),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Duration picker ───────────────────────────────────────────
                Text(
                    text = "How long?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = GkOnSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                DurationChipRow(
                    options = DURATION_OPTIONS,
                    selected = selectedDuration,
                    onSelect = { selectedDuration = it },
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Action buttons ────────────────────────────────────────────
                Button(
                    onClick = { onStart(intention.trim(), selectedDuration) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GkPrimary),
                ) {
                    Text(
                        text = "Start ${selectedDuration} min",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Not now",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = GkSubtle,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Duration chip row ─────────────────────────────────────────────────────────

@Composable
private fun DurationChipRow(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { minutes ->
            val isSelected = minutes == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(minutes) },
                label = {
                    Text(
                        text = if (minutes < 60) "${minutes}m" else "1h",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GkPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = GkSurface,
                    labelColor = GkSubtle,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = GkPrimary,
                    borderColor = GkSubtle.copy(alpha = 0.3f),
                ),
            )
        }
    }
}
