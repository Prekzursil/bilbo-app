package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

private const val FORM_PADDING_DP = 16
private const val FORM_GAP_DP = 8
private const val FORM_SECTION_GAP_DP = 16
private const val FORM_JOIN_GAP_DP = 20
private const val FORM_JOIN_PAD_DP = 24
private const val FORM_TINY_GAP_DP = 4
private const val FORM_ICON_DP = 18
private const val FORM_JOIN_ICON_DP = 56
private const val FORM_GOAL_MAX_LINES = 2
private const val FORM_DEFAULT_DURATION = 14
private const val FORM_DEFAULT_MAX_MEMBERS = 5
private const val FORM_DURATION_7 = 7
private const val FORM_DURATION_14 = 14
private const val FORM_DURATION_30 = 30
private const val FORM_MIN_MEMBERS = 3
private const val FORM_MAX_MEMBERS = 7
private const val FORM_INVITE_CODE_LEN = 8
private const val FORM_CODE_LETTER_SPACING = 4f
private val FORM_DURATION_OPTIONS = listOf(FORM_DURATION_7, FORM_DURATION_14, FORM_DURATION_30)
private val FORM_MAX_MEMBER_OPTIONS = (FORM_MIN_MEMBERS..FORM_MAX_MEMBERS).toList()

// ── Create circle form ────────────────────────────────────────────────────────

@Composable
internal fun CreateCircleForm(onCreate: (name: String, goal: String, durationDays: Int, maxMembers: Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(FORM_DEFAULT_DURATION) }
    var maxMembers by remember { mutableStateOf(FORM_DEFAULT_MAX_MEMBERS) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(FORM_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(FORM_SECTION_GAP_DP.dp),
    ) {
        item { CreateCircleHeader() }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Circle name") },
                placeholder = { Text("e.g. Morning Focus Group") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Goal (optional)") },
                placeholder = { Text("e.g. Under 2h screen time daily") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = FORM_GOAL_MAX_LINES,
            )
        }
        item {
            ChipPickerSection(title = "Duration", options = FORM_DURATION_OPTIONS, selected = duration) {
                duration = it
            }
        }
        item {
            ChipPickerSection(title = "Max Members", options = FORM_MAX_MEMBER_OPTIONS, selected = maxMembers) {
                maxMembers = it
            }
        }
        item {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, goal, duration, maxMembers) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(FORM_ICON_DP.dp))
                Spacer(Modifier.width(FORM_GAP_DP.dp))
                Text("Create Circle")
            }
        }
    }
}

@Composable
private fun CreateCircleHeader() {
    Text(
        "Create a Focus Circle",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(FORM_TINY_GAP_DP.dp))
    Text(
        "Invite friends to hold each other accountable.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChipPickerSection(
    title: String,
    options: List<Int>,
    selected: Int,
    label: (Int) -> String = { "$it" },
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FORM_GAP_DP.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
}

// ── Join circle form ──────────────────────────────────────────────────────────

@Composable
internal fun JoinCircleForm(onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(FORM_JOIN_PAD_DP.dp),
        verticalArrangement = Arrangement.spacedBy(FORM_JOIN_GAP_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Link,
            contentDescription = null,
            modifier = Modifier.size(FORM_JOIN_ICON_DP.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            "Enter Invite Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            "Ask the circle admin for their $FORM_INVITE_CODE_LEN-character invite code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= FORM_INVITE_CODE_LEN) code = it.uppercase() },
            placeholder = { Text("XXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle =
                MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = TextUnit(FORM_CODE_LETTER_SPACING, TextUnitType.Sp),
                ),
        )

        Button(
            onClick = { if (code.length == FORM_INVITE_CODE_LEN) onJoin(code) },
            modifier = Modifier.fillMaxWidth(),
            enabled = code.length == FORM_INVITE_CODE_LEN,
        ) {
            Text("Join Circle")
        }
    }
}
