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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant

// ── Layout / palette constants ────────────────────────────────────────────────
private const val ARGB_GOLD = 0xFFFFD700
private const val ARGB_SILVER = 0xFFC0C0C0
private const val ARGB_BRONZE = 0xFFCD7F32
private const val PODIUM_COUNT = 3
private const val RANK_GOLD = 1
private const val RANK_SILVER = 2
private const val RANK_BRONZE = 3
private const val NAME_MAX_CHARS = 12
private const val PODIUM_HEIGHT_1_DP = 100
private const val PODIUM_HEIGHT_2_DP = 72
private const val PODIUM_HEIGHT_3_DP = 52
private const val PODIUM_WIDTH_DP = 100
private const val MEDAL_SP = 24
private const val EDGE_PAD_DP = 16
private const val SCREEN_BOTTOM_PAD_DP = 32
private const val SECTION_GAP_DP = 8
private const val ROW_GAP_DP = 12
private const val PODIUM_GAP_DP = 6
private const val EMPTY_GAP_DP = 12
private const val EMPTY_PAD_DP = 40
private const val EMPTY_ICON_DP = 48
private const val ALPHA_DIVIDER = 0.2f
private const val ALPHA_EMPTY_ICON = 0.35f
private const val ALPHA_PODIUM_BAR = 0.2f
private const val ROW_CORNER_DP = 12
private const val ROW_TONAL_ELEVATION_DP = 1
private const val ALPHA_CURRENT_USER_BG = 0.35f
private const val ALPHA_AVATAR_BG = 0.15f
private const val ROW_PAD_H_DP = 14
private const val ROW_PAD_V_DP = 10
private const val ROW_OUTER_PAD_V_DP = 4
private const val RANK_WIDTH_DP = 32
private const val RANK_EMOJI_SP = 18
private const val AVATAR_DP = 36
private const val PODIUM_BAR_CORNER_DP = 6
private const val BANNER_CORNER_DP = 10
private const val BANNER_PAD_DP = 10
private const val BANNER_ICON_DP = 16

// ── UI models ─────────────────────────────────────────────────────────────────

data class LeaderboardUiState(
    val circleName: String = "",
    val entries: kotlin.collections.List<LeaderboardEntryUiItem> = emptyList(),
    val currentUserRank: Int? = null,
    val currentCategory: LeaderboardCategory = LeaderboardCategory.MOST_FP,
    val nextResetAt: Instant? = null,
    val isLoading: Boolean = false,
)

enum class LeaderboardCategory(
    val label: String,
) {
    MOST_FP("Most FP"),
    BEST_STREAK("Best Streak"),
    MOST_IMPROVED("Most Improved"),
    MOST_ANALOG("Most Analog Time"),
}

data class LeaderboardEntryUiItem(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val valueLabel: String, // e.g. "312 FP" or "7 days"
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
                    edgePadding = EDGE_PAD_DP.dp,
                ) {
                    LeaderboardCategory.entries.forEach { category ->
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
            LeaderboardList(state = state, modifier = Modifier.fillMaxSize().padding(paddingValues))
        }
    }
}

@Composable
private fun LeaderboardList(
    state: LeaderboardUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = SCREEN_BOTTOM_PAD_DP.dp),
    ) {
        if (state.nextResetAt != null) {
            item { WeeklyResetBanner() }
        }

        val top3 = state.entries.take(PODIUM_COUNT)
        if (top3.isNotEmpty()) {
            item {
                PodiumRow(top3 = top3)
                Spacer(Modifier.height(SECTION_GAP_DP.dp))
            }
        }

        item { AllRankingsHeader(currentUserRank = state.currentUserRank) }

        if (state.entries.isEmpty()) {
            item { EmptyLeaderboardState() }
        } else {
            items(state.entries) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
private fun AllRankingsHeader(currentUserRank: Int?) {
    Divider(
        modifier = Modifier.padding(horizontal = EDGE_PAD_DP.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_DIVIDER),
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EDGE_PAD_DP.dp, vertical = SECTION_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "All Rankings",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (currentUserRank != null) {
            Text(
                "You: #$currentUserRank",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyLeaderboardState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = EMPTY_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EMPTY_GAP_DP.dp),
    ) {
        Icon(
            Icons.Outlined.BarChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ALPHA_EMPTY_ICON),
            modifier = Modifier.size(EMPTY_ICON_DP.dp),
        )
        Text(
            "No data yet. Keep tracking your wellness!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Podium ────────────────────────────────────────────────────────────────────

@Composable
private fun PodiumRow(top3: kotlin.collections.List<LeaderboardEntryUiItem>) {
    val reordered =
        buildList {
            top3.getOrNull(1)?.let { add(it) } // 2nd (left)
            top3.getOrNull(0)?.let { add(it) } // 1st (center)
            top3.getOrNull(2)?.let { add(it) } // 3rd (right)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EDGE_PAD_DP.dp, vertical = EDGE_PAD_DP.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        reordered.forEach { entry ->
            PodiumSlot(entry = entry)
        }
    }
}

@Composable
private fun PodiumSlot(entry: LeaderboardEntryUiItem) {
    val (podiumHeight, medalEmoji, medalColor) =
        when (entry.rank) {
            RANK_GOLD -> Triple(PODIUM_HEIGHT_1_DP.dp, "🥇", Color(ARGB_GOLD))
            RANK_SILVER -> Triple(PODIUM_HEIGHT_2_DP.dp, "🥈", Color(ARGB_SILVER))
            RANK_BRONZE -> Triple(PODIUM_HEIGHT_3_DP.dp, "🥉", Color(ARGB_BRONZE))
            else -> Triple(PODIUM_HEIGHT_3_DP.dp, "#${entry.rank}", MaterialTheme.colorScheme.onSurfaceVariant)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PODIUM_GAP_DP.dp),
        modifier = Modifier.width(PODIUM_WIDTH_DP.dp),
    ) {
        Text(
            text = entry.displayName.take(NAME_MAX_CHARS),
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
        Text(text = medalEmoji, fontSize = MEDAL_SP.sp)

        // Podium bar
        Surface(
            shape = RoundedCornerShape(topStart = PODIUM_BAR_CORNER_DP.dp, topEnd = PODIUM_BAR_CORNER_DP.dp),
            color = medalColor.copy(alpha = ALPHA_PODIUM_BAR),
            modifier =
                Modifier
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
    val style = rememberLeaderboardRowStyle(entry.isCurrentUser)
    val rankEmoji = rankEmojiFor(entry.rank)

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EDGE_PAD_DP.dp, vertical = ROW_OUTER_PAD_V_DP.dp),
        shape = RoundedCornerShape(ROW_CORNER_DP.dp),
        color = style.background,
        tonalElevation = style.tonalElevation,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_PAD_H_DP.dp, vertical = ROW_PAD_V_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
        ) {
            LeaderboardRankBadge(rankEmoji = rankEmoji, rank = entry.rank)
            LeaderboardAvatar(displayName = entry.displayName, style = style)

            // Name
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = style.nameWeight,
                color = style.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )

            // Value
            Text(
                text = entry.valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = style.accentOrMuted,
                fontWeight = style.nameWeight,
            )
        }
    }
}

/** Resolved per-row visual styling, computed once so the row Composable stays simple. */
private data class LeaderboardRowStyle(
    val background: Color,
    val text: Color,
    val accent: Color,
    val accentOrMuted: Color,
    val avatarBackground: Color,
    val avatarText: Color,
    val nameWeight: FontWeight,
    val tonalElevation: androidx.compose.ui.unit.Dp,
)

@Composable
private fun rememberLeaderboardRowStyle(isCurrentUser: Boolean): LeaderboardRowStyle {
    val scheme = MaterialTheme.colorScheme
    return if (isCurrentUser) {
        LeaderboardRowStyle(
            background = scheme.primaryContainer.copy(alpha = ALPHA_CURRENT_USER_BG),
            text = scheme.primary,
            accent = scheme.primary,
            accentOrMuted = scheme.primary,
            avatarBackground = scheme.primary.copy(alpha = ALPHA_AVATAR_BG),
            avatarText = scheme.primary,
            nameWeight = FontWeight.Bold,
            tonalElevation = 0.dp,
        )
    } else {
        LeaderboardRowStyle(
            background = scheme.surface,
            text = scheme.onSurface,
            accent = scheme.primary,
            accentOrMuted = scheme.onSurfaceVariant,
            avatarBackground = scheme.secondaryContainer,
            avatarText = scheme.onSecondaryContainer,
            nameWeight = FontWeight.Normal,
            tonalElevation = ROW_TONAL_ELEVATION_DP.dp,
        )
    }
}

private fun rankEmojiFor(rank: Int): String? =
    when (rank) {
        RANK_GOLD -> "🥇"
        RANK_SILVER -> "🥈"
        RANK_BRONZE -> "🥉"
        else -> null
    }

@Composable
private fun LeaderboardRankBadge(
    rankEmoji: String?,
    rank: Int,
) {
    Box(
        modifier = Modifier.width(RANK_WIDTH_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rankEmoji != null) {
            Text(rankEmoji, fontSize = RANK_EMOJI_SP.sp)
        } else {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LeaderboardAvatar(
    displayName: String,
    style: LeaderboardRowStyle,
) {
    Surface(
        shape = CircleShape,
        color = style.avatarBackground,
        modifier = Modifier.size(AVATAR_DP.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = displayName.take(1).uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = style.avatarText,
            )
        }
    }
}

// ── Weekly reset banner ───────────────────────────────────────────────────────

@Composable
private fun WeeklyResetBanner() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EDGE_PAD_DP.dp, vertical = SECTION_GAP_DP.dp),
        shape = RoundedCornerShape(BANNER_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(BANNER_PAD_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(BANNER_ICON_DP.dp),
            )
            Text(
                text = "Leaderboard resets weekly on Sunday",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
