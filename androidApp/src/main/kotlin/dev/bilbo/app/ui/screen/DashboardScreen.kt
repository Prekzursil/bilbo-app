package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.bilbo.app.ui.theme.BilboTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bilbo") },
                actions = {
                    IconButton(onClick = onNavigateToInsights) {
                        Icon(Icons.Filled.Insights, contentDescription = "Insights")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ScreenTimeSummaryCard()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Today's Apps",
                style = MaterialTheme.typography.titleMedium,
            )
            // Preview rows until a DashboardViewModel surfaces real usage data.
            // Tracked by the UsageMonitorService → InsightRepository work.
            DEFAULT_PREVIEW_ROWS.forEach { (appName, minutes) ->
                AppUsageRow(appName = appName, durationMinutes = minutes)
            }
        }
    }
}

private val DEFAULT_PREVIEW_ROWS: List<Pair<String, Int>> = listOf(
    "Instagram" to 45,
    "YouTube" to 30,
    "Safari" to 20,
)

@Composable
private fun ScreenTimeSummaryCard() {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Screen Time",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "2h 35m",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "15 min under your daily goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppUsageRow(appName: String, durationMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(appName, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${durationMinutes}m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    BilboTheme {
        DashboardScreen(onNavigateToInsights = {}, onNavigateToSettings = {})
    }
}
