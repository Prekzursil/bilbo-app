package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bilbo.app.ui.dashboard.DashboardViewModel
import dev.bilbo.app.ui.dashboard.DashboardViewModel.AppUsage
import dev.bilbo.app.ui.dashboard.DashboardViewModel.DashboardUiState
import dev.bilbo.app.ui.theme.BilboTheme
import dev.bilbo.domain.AppCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreenContent(
        uiState = uiState,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreenContent(
    uiState: DashboardUiState,
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
                },
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.apps.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScreenTimeSummaryCard(uiState)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Today's Apps",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (uiState.apps.isEmpty()) {
                        EmptyAppsCard()
                    } else {
                        uiState.apps.forEach { app ->
                            AppUsageRow(app = app)
                        }
                    }
                    uiState.error?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenTimeSummaryCard(uiState: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Screen Time",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.formattedTotal,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = uiState.goalDeltaCopy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyAppsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "No usage recorded yet today",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Grant usage-access permission and keep the tracking service " +
                    "running to see live app totals appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(app.appLabel, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = app.category.displayLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${app.durationMinutes}m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun AppCategory.displayLabel(): String = when (this) {
    AppCategory.NUTRITIVE -> "Nutritive"
    AppCategory.NEUTRAL -> "Neutral"
    AppCategory.EMPTY_CALORIES -> "Empty calories"
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    BilboTheme {
        DashboardScreenContent(
            uiState = DashboardUiState(
                isLoading = false,
                totalMinutes = 135,
                apps = listOf(
                    AppUsage("com.instagram.android", "Instagram", 45, AppCategory.EMPTY_CALORIES),
                    AppUsage("com.google.android.youtube", "YouTube", 60, AppCategory.NEUTRAL),
                    AppUsage("com.duolingo", "Duolingo", 30, AppCategory.NUTRITIVE),
                ),
            ),
            onNavigateToInsights = {},
            onNavigateToSettings = {},
        )
    }
}
