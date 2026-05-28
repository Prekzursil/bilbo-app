package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Layout constants ──────────────────────────────────────────────────────────
private const val THOUSAND = 1_000
private const val MILLION = 1_000_000

/**
 * Weekly community digest card.
 *
 * Read-only, ~30-second read, with a dismiss button.
 * Shown as a full-screen sheet so it can be presented modally.
 *
 * @param data    Digest content from the backend.
 * @param onDismiss Called when the user taps "Got it" or closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigestScreen(
    data: CommunityDigestData = CommunityDigestData(),
    onDismiss: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Digest", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Button(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("Got it")
                }
            }
        },
    ) { paddingValues ->
        if (data.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            DigestList(data = data, modifier = Modifier.fillMaxSize().padding(paddingValues))
        }
    }
}

@Composable
private fun DigestList(
    data: CommunityDigestData,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { DigestHeroBanner(weekLabel = data.weekLabel) }
        item { DigestStatsRow(activeUsers = data.totalActiveUsers, hoursSaved = data.collectiveHoursSaved) }

        if (data.topAnalogSuggestion.isNotBlank()) {
            item {
                DigestHighlightCard(icon = "🌿", label = "Top Analog Pick", text = data.topAnalogSuggestion)
            }
        }
        if (data.topCircleAchievement.isNotBlank()) {
            item {
                DigestHighlightCard(icon = "🏆", label = "Circle Achievement", text = data.topCircleAchievement)
            }
        }
        if (data.anonymousTips.isNotEmpty()) {
            item {
                Text(
                    "From the Community",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(data.anonymousTips) { tip ->
                AnonymousTipCard(tip = tip)
            }
        }

        item { DigestFooter() }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DigestFooter() {
    Text(
        "This digest is compiled anonymously every Sunday. Individual data is never shared.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun DigestHeroBanner(weekLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🌍", fontSize = 36.sp)
            Text(
                text = "This Week in the Bilbo Community",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DigestStatsRow(
    activeUsers: Int,
    hoursSaved: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Active users card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "👥", fontSize = 24.sp)
                Text(
                    text = formatCompact(activeUsers),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "active users",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Hours saved card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "⏱️", fontSize = 24.sp)
                Text(
                    text = "${formatCompact(hoursSaved)}h",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "hours reclaimed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DigestHighlightCard(
    icon: String,
    label: String,
    text: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = icon, fontSize = 24.sp, modifier = Modifier.padding(top = 2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AnonymousTipCard(tip: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "💬", fontSize = 18.sp)
            Text(
                text = "\"$tip\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Helpers / models ──────────────────────────────────────────────────────────

private fun formatCompact(n: Int): String =
    when {
        n >= MILLION -> "${n / MILLION}M"
        n >= THOUSAND -> "${n / THOUSAND}K"
        else -> n.toString()
    }

data class CommunityDigestData(
    val weekLabel: String = "Apr 7–13", // e.g. "Apr 7–13"
    val totalActiveUsers: Int = 0,
    val collectiveHoursSaved: Int = 0,
    val topAnalogSuggestion: String = "",
    val topCircleAchievement: String = "",
    val anonymousTips: kotlin.collections.List<String> = emptyList(), // max 3
    val isLoading: Boolean = false,
)
