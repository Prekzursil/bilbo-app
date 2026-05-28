package dev.bilbo.app.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings sub-screen showing the exact JSON payload that would be sent to the AI service.
 *
 * Displays:
 *  - A clear disclosure statement.
 *  - The raw anonymized JSON payload in a monospace scrollable card.
 *  - Toggle to enable/disable cloud AI insights (default off).
 *  - "Refresh preview" button to regenerate the payload from current data.
 *  - Privacy reassurance text at bottom.
 *
 * @param jsonPayload         The current anonymized JSON string to display.
 * @param isCloudAiEnabled    Whether cloud AI insights are currently enabled.
 * @param isRefreshingPayload True while the payload is being regenerated.
 * @param onToggleCloudAi     Called when the user toggles the cloud AI switch.
 * @param onRefreshPayload    Called when the user taps "Refresh preview".
 * @param onBack              Navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAnonymizationScreen(
    jsonPayload: String = SAMPLE_PAYLOAD,
    isCloudAiEnabled: Boolean = false,
    isRefreshingPayload: Boolean = false,
    onToggleCloudAi: (Boolean) -> Unit = {},
    onRefreshPayload: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val verticalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Privacy",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(verticalScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Disclosure banner ─────────────────────────────────────────────
            DisclosureBanner()

            // ── Cloud AI toggle ───────────────────────────────────────────────
            CloudAiToggleCard(
                isEnabled = isCloudAiEnabled,
                onToggle = onToggleCloudAi,
            )

            // ── JSON payload card ─────────────────────────────────────────────
            JsonPayloadCard(
                jsonPayload = jsonPayload,
                isRefreshing = isRefreshingPayload,
                onRefresh = onRefreshPayload,
            )

            // ── Privacy reassurance ───────────────────────────────────────────
            PrivacyReassuranceCard()

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun DisclosureBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Full Transparency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text =
                    "This is exactly what we send to the AI service — no more, no less. " +
                        "All data is aggregated and anonymized. Your name, device ID, and " +
                        "specific app names are never included.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CloudAiToggleCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Enable AI-powered insights",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "Send anonymized weekly data to generate a personalized narrative. " +
                            "Rate-limited to once per week.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun JsonPayloadCard(
    jsonPayload: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            JsonPayloadHeader(isRefreshing = isRefreshing, onRefresh = onRefresh)

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Scrollable monospace JSON display
            val hScrollState = rememberScrollState()
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(hScrollState)
                            .padding(12.dp),
                ) {
                    Text(
                        text = jsonPayload.formatJson(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonPayloadHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Payload Preview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(text = "Refresh preview", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PrivacyReassuranceCard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .size(18.dp)
                    .padding(top = 2.dp),
        )
        Text(
            text =
                "Your data stays on your device. Only the aggregated statistics shown above " +
                    "are ever transmitted — and only when you enable AI insights. The AI service " +
                    "cannot identify you from this data. You can disable this at any time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Minimal pretty-printer: adds newlines after each comma/brace at the top level.
 * Good enough for display purposes without pulling in a JSON library.
 */
private class JsonPrettyPrinter {
    private val sb = StringBuilder()
    private var indent = 0

    private fun newlineIndent() {
        sb.append('\n')
        sb.append("  ".repeat(indent))
    }

    fun append(
        c: Char,
        inString: Boolean,
    ) {
        if (inString) sb.append(c) else appendStructural(c)
    }

    private fun appendStructural(c: Char) {
        when (c) {
            '{', '[' -> {
                sb.append(c)
                indent++
                newlineIndent()
            }
            '}', ']' -> {
                indent--
                newlineIndent()
                sb.append(c)
            }
            ',' -> {
                sb.append(c)
                newlineIndent()
            }
            ':' -> sb.append("$c ")
            ' ' -> Unit // skip raw spaces outside strings
            else -> sb.append(c)
        }
    }

    override fun toString(): String = sb.toString()
}

private fun String.formatJson(): String {
    val printer = JsonPrettyPrinter()
    var inString = false
    forEachIndexed { i, c ->
        inString = togglesStringState(c, prevChar(i), inString)
        printer.append(c, inString)
    }
    return printer.toString()
}

private fun String.prevChar(index: Int): Char = if (index > 0) this[index - 1] else ' '

private fun togglesStringState(
    c: Char,
    prev: Char,
    inString: Boolean,
): Boolean = if (c == '"' && prev != '\\') !inString else inString

// ── Sample payload for previews ───────────────────────────────────────────────

private const val SAMPLE_PAYLOAD =
    """{"weekStart":"2026-04-07","totalScreenTimeMinutes":312,""" +
        """"emptyCalorieMinutes":148,"nutritiveMinutes":87,"neutralMinutes":77,""" +
        """"fpEarned":105,"fpSpent":60,"fpBalance":60,"intentAccuracyPercent":0.78,""" +
        """"streakDays":5,"topEmotions":["BORED","STRESSED","RELAXED"],""" +
        """"spikeDays":["MONDAY"],""" +
        """"heuristicInsightTypes":["ACHIEVEMENT","CORRELATION","TREND"],""" +
        """"weekOverWeekChange":-0.12,"topNutritiveApps":["Duolingo","Kindle","Calm"],""" +
        """"topEmptyCalorieApps":["Instagram","TikTok","YouTube"]}"""
