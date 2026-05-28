package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bilbo.social.ChallengeEngine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val FORM_PADDING_DP = 16
private const val FORM_GAP_DP = 8
private const val FORM_ICON_DP = 18
private const val FORM_DURATION_3 = 3
private const val FORM_DURATION_7 = 7
private const val FORM_DURATION_14 = 14
private const val FORM_DURATION_30 = 30
private val FORM_DURATION_OPTIONS = listOf(FORM_DURATION_3, FORM_DURATION_7, FORM_DURATION_14, FORM_DURATION_30)
private const val FORM_DEFAULT_DURATION_DAYS = 7

// ── Create challenge form ─────────────────────────────────────────────────────

@Composable
internal fun CreateChallengeForm(
    onCreate: (String, ChallengeEngine.ChallengeType, Boolean, Int, LocalDate, LocalDate) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES) }
    var isTeam by remember { mutableStateOf(false) }
    var targetValue by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf(FORM_DEFAULT_DURATION_DAYS) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(FORM_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(FORM_PADDING_DP.dp),
    ) {
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Challenge title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item { ChallengeTypePicker(selectedType = selectedType, onSelect = { selectedType = it }) }
        item { ChallengeModePicker(isTeam = isTeam, onSelect = { isTeam = it }) }
        item {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { if (it.all(Char::isDigit)) targetValue = it },
                label = { Text("Target value (${selectedType.unit()})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item { ChallengeDurationPicker(durationDays = durationDays, onSelect = { durationDays = it }) }
        item {
            CreateChallengeButton(
                enabled = title.isNotBlank() && targetValue.toIntOrNull() != null,
                onClick = {
                    submitChallenge(
                        title = title,
                        targetText = targetValue,
                        type = selectedType,
                        isTeam = isTeam,
                        durationDays = durationDays,
                        onCreate = onCreate,
                    )
                },
            )
        }
    }
}

private fun submitChallenge(
    title: String,
    targetText: String,
    type: ChallengeEngine.ChallengeType,
    isTeam: Boolean,
    durationDays: Int,
    onCreate: (String, ChallengeEngine.ChallengeType, Boolean, Int, LocalDate, LocalDate) -> Unit,
) {
    val target = targetText.toIntOrNull() ?: return
    if (title.isBlank() || target <= 0) return
    val today =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    val endDate = today.plus(DatePeriod(days = durationDays))
    onCreate(title, type, isTeam, target, today, endDate)
}

@Composable
private fun ChallengeTypePicker(
    selectedType: ChallengeEngine.ChallengeType,
    onSelect: (ChallengeEngine.ChallengeType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
        Text("Type", style = MaterialTheme.typography.labelMedium)
        ChallengeEngine.ChallengeType.entries.forEach { type ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedType == type, onClick = { onSelect(type) })
                Column {
                    Text(type.displayName(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        type.description(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeModePicker(
    isTeam: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
        Text("Mode", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
            FilterChip(selected = !isTeam, onClick = { onSelect(false) }, label = { Text("Competitive") })
            FilterChip(selected = isTeam, onClick = { onSelect(true) }, label = { Text("Cooperative") })
        }
    }
}

@Composable
private fun ChallengeDurationPicker(
    durationDays: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
        Text("Duration", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
            FORM_DURATION_OPTIONS.forEach { days ->
                FilterChip(
                    selected = durationDays == days,
                    onClick = { onSelect(days) },
                    label = { Text("${days}d") },
                )
            }
        }
    }
}

@Composable
private fun CreateChallengeButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(FORM_ICON_DP.dp))
        Spacer(Modifier.width(FORM_GAP_DP.dp))
        Text("Create Challenge")
    }
}
