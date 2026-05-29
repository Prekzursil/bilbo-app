package dev.bilbo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bilbo.social.BuddyManager
import dev.bilbo.social.ChallengeEngine

// ── Sharing-level chip colours ────────────────────────────────────────────────
private const val ARGB_BASIC = 0xFF2196F3
private const val ARGB_DETAILED = 0xFF4CAF50

private const val TAB_BUDDIES = 0
private const val TAB_CIRCLES = 1
private const val TAB_CHALLENGES = 2

private const val LIST_CARD_CORNER_DP = 14
private const val LIST_CARD_ELEVATION_DP = 2
private const val LIST_CARD_PADDING_DP = 14
private const val LIST_CARD_SPACING_DP = 12

@Composable
private fun SocialListCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LIST_CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LIST_CARD_ELEVATION_DP.dp),
    ) {
        content()
    }
}

@Composable
private fun SocialListRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(LIST_CARD_PADDING_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LIST_CARD_SPACING_DP.dp),
        content = content,
    )
}

// ── UI state models ───────────────────────────────────────────────────────────

data class BuddyUiState(
    val pairs: List<BuddyPairUiItem> = emptyList(),
    val pendingInviteCode: String? = null,
)

data class BuddyPairUiItem(
    val pairId: String,
    val buddyDisplayName: String,
    val sharingLevel: BuddyManager.SharingLevel,
    val statusSummary: String, // e.g. "312 FP · 5-day streak"
)

data class CircleUiState(
    val circles: List<CircleUiItem> = emptyList(),
)

data class CircleUiItem(
    val circleId: String,
    val name: String,
    val memberCount: Int,
    val goalSummary: String,
)

data class ChallengeUiState(
    val activeChallenges: List<ChallengeUiItem> = emptyList(),
)

data class ChallengeUiItem(
    val challengeId: String,
    val title: String,
    val type: ChallengeEngine.ChallengeType,
    val progressPercent: Int,
    val daysRemaining: Int,
    val isTeam: Boolean,
)

/**
 * Main Social tab — three sub-sections via a TabRow:
 *  1. Buddies — accountability pairs
 *  2. Circles — focus groups
 *  3. Challenges — active challenges
 *
 * Navigation to detail screens is via the provided callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialHubScreen(
    buddyState: BuddyUiState = BuddyUiState(),
    circleState: CircleUiState = CircleUiState(),
    challengeState: ChallengeUiState = ChallengeUiState(),
    onInviteBuddy: () -> Unit = {},
    onEnterBuddyCode: () -> Unit = {},
    onBuddyPairTap: (String) -> Unit = {},
    onCreateCircle: () -> Unit = {},
    onJoinCircle: () -> Unit = {},
    onCircleTap: (String) -> Unit = {},
    onCreateChallenge: () -> Unit = {},
    onChallengeTap: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(TAB_BUDDIES) }

    Scaffold(
        topBar = {
            SocialHubTopBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (selectedTab) {
                TAB_BUDDIES ->
                    BuddiesTab(
                        state = buddyState,
                        onInvite = onInviteBuddy,
                        onEnterCode = onEnterBuddyCode,
                        onPairTap = onBuddyPairTap,
                    )
                TAB_CIRCLES ->
                    CirclesTab(
                        state = circleState,
                        onCreate = onCreateCircle,
                        onJoin = onJoinCircle,
                        onCircleTap = onCircleTap,
                    )
                TAB_CHALLENGES ->
                    ChallengesTab(
                        state = challengeState,
                        onCreate = onCreateChallenge,
                        onChallengeTap = onChallengeTap,
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialHubTopBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val tabs =
        listOf(
            Triple("Buddies", Icons.Outlined.People, Icons.Filled.People),
            Triple("Circles", Icons.Outlined.Group, Icons.Filled.Group),
            Triple("Challenges", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
        )
    Column {
        TopAppBar(
            title = { Text("Social", fontWeight = FontWeight.SemiBold) },
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
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            tabs.forEachIndexed { idx, (label, outlinedIcon, filledIcon) ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { onTabSelect(idx) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == idx) filledIcon else outlinedIcon,
                            contentDescription = label,
                        )
                    },
                    text = { Text(label) },
                )
            }
        }
    }
}

// ── Buddies tab ───────────────────────────────────────────────────────────────

@Composable
private fun BuddiesTab(
    state: BuddyUiState,
    onInvite: () -> Unit,
    onEnterCode: () -> Unit,
    onPairTap: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onInvite,
                    modifier = Modifier.weight(1f),
                    enabled = state.pairs.size < BuddyManager.MAX_BUDDY_PAIRS,
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Invite Buddy")
                }
                OutlinedButton(
                    onClick = onEnterCode,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enter Code")
                }
            }
        }

        if (state.pairs.isEmpty()) {
            item {
                SocialEmptyState(
                    icon = Icons.Outlined.People,
                    message = "No buddies yet. Invite a friend to get started!",
                )
            }
        } else {
            items(state.pairs, key = { it.pairId }) { pair ->
                BuddyPairListItem(pair = pair, onClick = { onPairTap(pair.pairId) })
            }
        }
    }
}

@Composable
private fun BuddyPairListItem(
    pair: BuddyPairUiItem,
    onClick: () -> Unit,
) {
    SocialListCard(onClick = onClick) {
        SocialListRow {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = pair.buddyDisplayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pair.buddyDisplayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    pair.statusSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SharingLevelChip(level = pair.sharingLevel)

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SharingLevelChip(level: BuddyManager.SharingLevel) {
    val (label, color) =
        when (level) {
            BuddyManager.SharingLevel.MINIMAL -> "Minimal" to MaterialTheme.colorScheme.outline
            BuddyManager.SharingLevel.BASIC -> "Basic" to Color(ARGB_BASIC)
            BuddyManager.SharingLevel.STANDARD -> "Standard" to MaterialTheme.colorScheme.primary
            BuddyManager.SharingLevel.DETAILED -> "Detailed" to Color(ARGB_DETAILED)
        }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ── Circles tab ───────────────────────────────────────────────────────────────

@Composable
private fun CirclesTab(
    state: CircleUiState,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onCircleTap: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Circle")
                }
                OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Join Circle")
                }
            }
        }

        if (state.circles.isEmpty()) {
            item {
                SocialEmptyState(
                    icon = Icons.Outlined.Group,
                    message = "No circles yet. Create one or join with an invite code!",
                )
            }
        } else {
            items(state.circles, key = { it.circleId }) { circle ->
                CircleListItem(circle = circle, onClick = { onCircleTap(circle.circleId) })
            }
        }
    }
}

@Composable
private fun CircleListItem(
    circle: CircleUiItem,
    onClick: () -> Unit,
) {
    SocialListCard(onClick = onClick) {
        SocialListRow {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(circle.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${circle.memberCount} member${if (circle.memberCount != 1) "s" else ""} · ${circle.goalSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Challenges tab ────────────────────────────────────────────────────────────

@Composable
private fun ChallengesTab(
    state: ChallengeUiState,
    onCreate: () -> Unit,
    onChallengeTap: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Challenge")
            }
        }

        if (state.activeChallenges.isEmpty()) {
            item {
                SocialEmptyState(
                    icon = Icons.Outlined.EmojiEvents,
                    message = "No active challenges. Create one to get motivated!",
                )
            }
        } else {
            items(state.activeChallenges, key = { it.challengeId }) { challenge ->
                ChallengeListItem(challenge = challenge, onClick = { onChallengeTap(challenge.challengeId) })
            }
        }
    }
}

@Composable
private fun ChallengeListItem(
    challenge: ChallengeUiItem,
    onClick: () -> Unit,
) {
    SocialListCard(onClick = onClick) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(LIST_CARD_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${if (challenge.isTeam) "Team" else "Individual"} · ${challenge.daysRemaining}d left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            LinearProgressIndicator(
                progress = { challenge.progressPercent / 100f },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )

            Text(
                "${challenge.progressPercent}% complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Shared empty state ────────────────────────────────────────────────────────

@Composable
private fun SocialEmptyState(
    icon: ImageVector,
    message: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

// ── Extension items for LazyColumn ───────────────────────────────────────────

private fun <T : Any> LazyListScope.items(
    items: List<T>,
    key: ((T) -> Any)? = null,
    content: @Composable LazyItemScope.(T) -> Unit,
) {
    items(count = items.size, key = key?.let { k -> { k(items[it]) } }) { index ->
        content(items[index])
    }
}
