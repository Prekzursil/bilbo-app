package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bilbo.social.LeaderboardCalculator
import kotlinx.datetime.Instant

// ── UI models ─────────────────────────────────────────────────────────────────

data class LeaderboardUiState(
    val circleName: String = "",
    val entries: kotlin.collections.List<LeaderboardEntryUiItem> = emptyList(),
    val currentUserRank: Int? = null,
    val currentCategory: LeaderboardCategory = LeaderboardCategory.MOST_FP,
    val nextResetAt: Instant? = null,
    val isLoading: Boolean = false,
)

enum class LeaderboardCategory(val label: String) {
    MOST_FP("Most FP"),
    BEST_STREAK("Best Streak"),
    MOST_IMPROVED("Most Improved"),
    MOST_ANALOG("Most Analog Time"),
}

data class LeaderboardEntryUiItem(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val valueLabel: String,       // e.g. "312 FP" or "7 days"
    val isCurrentUser: Boolean,
)

/**
 * Circle-scoped leaderboard screen.
 *
 * Features:
 *  - Category tabs: Most FP · Best Streak · Most Improved · Most Analog Time
 *  - Podium for ranks 1–3 with medal badges
 *  - Ranked list with current user highlighted
 *  - Weekly reset indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState = LeaderboardUiState(),
    onCategoryChange: (LeaderboardCategory) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Leaderboard", fontWeight = FontWeight.SemiBold)
                            if (state.circleName.isNotBlank()) {
                                Text(
                                    state.circleName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                ScrollableTabRow(
                    selectedTabIndex = LeaderboardCategory.entries.indexOf(state.currentCategory),
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 16.dp,
                ) {
                    LeaderboardCategory.entries.forEachIndexed { idx, category ->
                        Tab(
                            selected = state.currentCategory == category,
                            onClick = { onCategoryChange(category) },
                            text = { Text(category.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Weekly reset indicator
                state.nextResetAt?.let { resetAt ->
                    item {
                        WeeklyResetBanner(nextReset = resetAt)
                    }
                }

                // Podium (top 3)
                val top3 = state.entries.take(3)
                if (top3.isNotEmpty()) {
                    item {
                        PodiumRow(top3 = top3, category = state.currentCategory)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Divider + "All" header
                item {
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("All Rankings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.currentUserRank?.let {
                            Text("You: #$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Full ranked list
                if (state.entries.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(48.dp))
                            Text("No data yet. Keep tracking your wellness!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(state.entries) { entry ->
                        LeaderboardRow(entry = entry)
                    }
                }
            }
        }
    }
}

// ── Podium ────────────────────────────────────────────────────────────────────

@Composable
private fun PodiumRow(
    top3: kotlin.collections.List<LeaderboardEntryUiItem>,
    category: LeaderboardCategory,
) {
    val reordered = buildList {
        top3.getOrNull(1)?.let { add(it) }  // 2nd (left)
        top3.getOrNull(0)?.let { add(it) }  // 1st (center)
        top3.getOrNull(2)?.let { add(it) }  // 3rd (right)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        reordered.forEach { entry ->
            PodiumSlot(entry = entry, category = category)
        }
    }
}

@Composable
private fun PodiumSlot(entry: LeaderboardEntryUiItem, category: LeaderboardCategory) {
    val (podiumHeight, medalEmoji, medalColor) = when (entry.rank) {
        1 -> Triple(100.dp, "🥇", Color(0xFFFFD700))
        2 -> Triple(72.dp, "🥈", Color(0xFFC0C0C0))
        3 -> Triple(52.dp, "🥉", Color(0xFFCD7F32))
        else -> Triple(52.dp, "#${entry.rank}", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(100.dp),
    ) {
        Text(
            text = entry.displayName.take(12),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = entry.valueLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(text = medalEmoji, fontSize = 24.sp)

        // Podium bar
        Surface(
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
            color = medalColor.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    color = medalColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Ranked list row ───────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryUiItem) {
    val (bgColor, textColor) = if (entry.isCurrentUser)
        Pair(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), MaterialTheme.colorScheme.primary)
    else
        Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)

    val rankEmoji = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = if (entry.isCurrentUser) 0.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Rank badge
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (rankEmoji != null) {
                    Text(rankEmoji, fontSize = 18.sp)
                } else {
                    Text(
                        text = "#${entry.rank}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Avatar
            Surface(
                shape = CircleShape,
                color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            // Name
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )

            // Value
            Text(
                text = entry.valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

// ── Weekly reset banner ───────────────────────────────────────────────────────

@Composable
private fun WeeklyResetBanner(nextReset: Instant) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Leaderboard resets weekly on Sunday",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
