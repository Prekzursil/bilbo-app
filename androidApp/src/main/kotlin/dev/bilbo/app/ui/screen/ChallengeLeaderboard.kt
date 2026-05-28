package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ── Leaderboard rendering (competitive challenges) ────────────────────────────
private const val LB_ARGB_GOLD = 0xFFFFD700
private const val LB_ARGB_SILVER = 0xFFC0C0C0
private const val LB_ARGB_BRONZE = 0xFFCD7F32
private const val LB_RANK_GOLD = 1
private const val LB_RANK_SILVER = 2
private const val LB_RANK_BRONZE = 3
private const val LB_LIMIT = 5
private const val LB_ROW_CORNER_DP = 12
private const val LB_RANK_WIDTH_DP = 32
private const val LB_TONAL_ELEVATION_DP = 2
private const val LB_ALPHA_CURRENT_USER = 0.4f

internal fun LazyListScope.leaderboardSection(
    leaderboard: kotlin.collections.List<ChallengeLeaderboardEntry>,
) {
    if (leaderboard.isEmpty()) return
    item {
        Text("Leaderboard", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
    items(leaderboard.take(LB_LIMIT)) { entry ->
        ChallengeLeaderboardRow(entry = entry)
    }
}

private fun rankColorArgb(rank: Int): Long? =
    when (rank) {
        LB_RANK_GOLD -> LB_ARGB_GOLD
        LB_RANK_SILVER -> LB_ARGB_SILVER
        LB_RANK_BRONZE -> LB_ARGB_BRONZE
        else -> null
    }

private fun rankEmoji(rank: Int): String =
    when (rank) {
        LB_RANK_GOLD -> "🥇"
        LB_RANK_SILVER -> "🥈"
        LB_RANK_BRONZE -> "🥉"
        else -> "#$rank"
    }

@Composable
private fun ChallengeLeaderboardRow(entry: ChallengeLeaderboardEntry) {
    val rankColor = rankColorArgb(entry.rank)?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val rowColor =
        if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = LB_ALPHA_CURRENT_USER)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        shape = RoundedCornerShape(LB_ROW_CORNER_DP.dp),
        color = rowColor,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = if (entry.isCurrentUser) 0.dp else LB_TONAL_ELEVATION_DP.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(LB_ROW_CORNER_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LB_ROW_CORNER_DP.dp),
        ) {
            Text(
                rankEmoji(entry.rank),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(LB_RANK_WIDTH_DP.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                entry.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                entry.progressLabel,
                style = MaterialTheme.typography.bodySmall,
                color = rankColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
