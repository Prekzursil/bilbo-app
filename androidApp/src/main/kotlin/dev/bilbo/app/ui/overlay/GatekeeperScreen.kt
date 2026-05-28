package dev.bilbo.app.ui.overlay

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
import androidx.compose.runtime.Stable
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
import dev.bilbo.tracking.AppInfo

// ── Brand palette for the gatekeeper overlay ──────────────────────────────────
private const val ARGB_BACKGROUND = 0x99000000

private val GkBackground = Color(ARGB_BACKGROUND) // scrim
private val GkCard = OverlayPalette.Background // dark teal-navy card
private val GkPrimary = OverlayPalette.Primary // calming teal
private val GkSurface = OverlayPalette.Surface // slightly lighter surface
private val GkOnSurface = OverlayPalette.OnSurface // cool white text
private val GkSubtle = OverlayPalette.Subtle // muted label

private const val DURATION_5 = 5
private const val DURATION_10 = 10
private const val DURATION_15 = 15
private const val DURATION_20 = 20
private const val DURATION_30 = 30
private const val DURATION_60 = 60
private val DURATION_OPTIONS = listOf(DURATION_5, DURATION_10, DURATION_15, DURATION_20, DURATION_30, DURATION_60)

private const val MAX_INTENTION_LEN = 100
private const val SLIDE_DURATION_MS = 380
private const val CARD_CORNER_DP = 28
private const val CARD_ELEVATION_DP = 12
private const val CARD_PAD_H_DP = 24
private const val CARD_PAD_V_DP = 28
private const val HANDLE_WIDTH_DP = 40
private const val HANDLE_HEIGHT_DP = 4
private const val HANDLE_RADIUS_DP = 2
private const val ALPHA_HANDLE = 0.5f
private const val ICON_SIZE_DP = 56
private const val ALPHA_ICON_FILL = 0.18f
private const val ICON_LABEL_CHARS = 2
private const val FIELD_CORNER_DP = 14
private const val FIELD_MAX_LINES = 3
private const val ALPHA_FIELD_BORDER = 0.4f
private const val BUTTON_HEIGHT_DP = 52
private const val CHIP_SPACING_DP = 6
private const val CHIP_CORNER_DP = 20
private const val ALPHA_CHIP_BORDER = 0.3f
private const val SPACE_LG_DP = 24
private const val SPACE_MD_DP = 20
private const val SPACE_SM_DP = 12
private const val SPACE_XS_DP = 10
private const val SPACE_TINY_DP = 8
private const val SPACE_XL_DP = 28

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
    val form = remember { GatekeeperFormState() }

    // Animate the card sliding up from y-offset +100% → 0
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val slideProgress by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(durationMillis = SLIDE_DURATION_MS),
        label = "GatekeeperSlide",
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(GkBackground),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = size.height * slideProgress
                    },
            shape = RoundedCornerShape(topStart = CARD_CORNER_DP.dp, topEnd = CARD_CORNER_DP.dp),
            colors = CardDefaults.cardColors(containerColor = GkCard),
            elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
        ) {
            GatekeeperCardContent(
                appLabel = appInfo.appLabel,
                form = form,
                onStart = { onStart(form.intention.trim(), form.selectedDuration) },
                onDismiss = onDismiss,
            )
        }
    }
}

/** Hoisted, mutable form state for the gatekeeper card (intention text + chosen duration). */
@Stable
class GatekeeperFormState {
    var intention by mutableStateOf("")
    var selectedDuration by mutableStateOf(DURATION_15)
}

@Composable
private fun GatekeeperCardContent(
    appLabel: String,
    form: GatekeeperFormState,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CARD_PAD_H_DP.dp, vertical = CARD_PAD_V_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragHandle()
        Spacer(modifier = Modifier.height(SPACE_MD_DP.dp))
        AppHeader(appLabel = appLabel)
        Spacer(modifier = Modifier.height(SPACE_LG_DP.dp))
        SectionLabel(text = "What's your intention?")
        Spacer(modifier = Modifier.height(SPACE_TINY_DP.dp))
        IntentionField(value = form.intention, onValueChange = { form.intention = it })
        Spacer(modifier = Modifier.height(SPACE_MD_DP.dp))
        SectionLabel(text = "How long?")
        Spacer(modifier = Modifier.height(SPACE_XS_DP.dp))
        DurationChipRow(
            options = DURATION_OPTIONS,
            selected = form.selectedDuration,
            onSelect = { form.selectedDuration = it },
        )
        Spacer(modifier = Modifier.height(SPACE_XL_DP.dp))
        StartButton(durationMinutes = form.selectedDuration, onStart = onStart)
        Spacer(modifier = Modifier.height(SPACE_TINY_DP.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Not now",
                style = MaterialTheme.typography.bodyLarge.copy(color = GkSubtle),
            )
        }
        Spacer(modifier = Modifier.height(SPACE_TINY_DP.dp))
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier =
            Modifier
                .size(width = HANDLE_WIDTH_DP.dp, height = HANDLE_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(HANDLE_RADIUS_DP.dp))
                .background(GkSubtle.copy(alpha = ALPHA_HANDLE)),
    )
}

@Composable
private fun AppHeader(appLabel: String) {
    Box(
        modifier =
            Modifier
                .size(ICON_SIZE_DP.dp)
                .clip(CircleShape)
                .background(GkPrimary.copy(alpha = ALPHA_ICON_FILL)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = appLabel.take(ICON_LABEL_CHARS).uppercase(),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GkPrimary,
                ),
        )
    }

    Spacer(modifier = Modifier.height(SPACE_SM_DP.dp))

    Text(
        text = appLabel,
        style =
            MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = GkOnSurface,
            ),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.titleMedium.copy(
                color = GkOnSurface,
                fontWeight = FontWeight.Medium,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun IntentionField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= MAX_INTENTION_LEN) onValueChange(it) },
        placeholder = {
            Text(
                text = "e.g. check my messages (optional)",
                style = MaterialTheme.typography.bodyMedium.copy(color = GkSubtle),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FIELD_CORNER_DP.dp),
        singleLine = false,
        maxLines = FIELD_MAX_LINES,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GkPrimary,
                unfocusedBorderColor = GkSubtle.copy(alpha = ALPHA_FIELD_BORDER),
                focusedTextColor = GkOnSurface,
                unfocusedTextColor = GkOnSurface,
                cursorColor = GkPrimary,
                focusedContainerColor = GkSurface,
                unfocusedContainerColor = GkSurface,
            ),
        supportingText = {
            Text(
                text = "${value.length}/$MAX_INTENTION_LEN",
                style = MaterialTheme.typography.labelSmall.copy(color = GkSubtle),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
    )
}

@Composable
private fun StartButton(
    durationMinutes: Int,
    onStart: () -> Unit,
) {
    Button(
        onClick = onStart,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT_DP.dp),
        shape = RoundedCornerShape(FIELD_CORNER_DP.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GkPrimary),
    ) {
        Text(
            text = "Start $durationMinutes min",
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
        )
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
        horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING_DP.dp),
    ) {
        options.forEach { minutes ->
            DurationChip(
                minutes = minutes,
                isSelected = minutes == selected,
                onClick = { onSelect(minutes) },
            )
        }
    }
}

private fun durationLabel(minutes: Int): String = if (minutes < DURATION_60) "${minutes}m" else "1h"

@Composable
private fun DurationChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = durationLabel(minutes),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
            )
        },
        shape = RoundedCornerShape(CHIP_CORNER_DP.dp),
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = GkPrimary,
                selectedLabelColor = Color.White,
                containerColor = GkSurface,
                labelColor = GkSubtle,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                selectedBorderColor = GkPrimary,
                borderColor = GkSubtle.copy(alpha = ALPHA_CHIP_BORDER),
            ),
    )
}
