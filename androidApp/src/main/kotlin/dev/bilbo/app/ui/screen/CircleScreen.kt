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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ── Layout constants ──────────────────────────────────────────────────────────
private const val PERCENT_MAX = 100f
private const val PADDING_DP = 16
private const val LIST_GAP_DP = 12
private const val SECTION_GAP_DP = 16
private const val SMALL_GAP_DP = 8
private const val TINY_GAP_DP = 6
private const val ROW_GAP_DP = 10
private const val CARD_PAD_DP = 14
private const val ICON_SIZE_DP = 18
private const val AVATAR_LG_DP = 48
private const val AVATAR_SM_DP = 40
private const val EMPTY_ICON_DP = 48
private const val EMPTY_PAD_DP = 40
private const val LOADING_PAD_DP = 32
private const val CARD_CORNER_DP = 16
private const val CARD_CORNER_MD_DP = 14
private const val CARD_CORNER_SM_DP = 12
private const val CHIP_CORNER_SM_DP = 6
private const val CARD_ELEVATION_DP = 2
private const val PROGRESS_HEIGHT_DP = 10
private const val PROGRESS_RADIUS_DP = 5
private const val ALPHA_EMPTY_ICON = 0.35f
private const val ALPHA_SUBTLE = 0.7f
private const val ALPHA_TRACK = 0.2f
private const val ALPHA_CURRENT_USER = 0.3f
private const val ALPHA_BADGE = 0.12f
private const val BADGE_PAD_H_DP = 5
private const val BADGE_PAD_V_DP = 2

// ── UI models ─────────────────────────────────────────────────────────────────

sealed class CircleScreenMode {
    /** Show the list of user's circles. */
    data object List : CircleScreenMode()

    /** Detail view for a specific circle. */
    data class Detail(
        val circleId: String,
    ) : CircleScreenMode()

    /** Create circle form. */
    data object Create : CircleScreenMode()

    /** Join circle by code. */
    data object Join : CircleScreenMode()
}

data class CircleDetailUiState(
    val circles: kotlin.collections.List<CircleListUiItem> = emptyList(),
    val selectedCircle: CircleDetailItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class CircleListUiItem(
    val circleId: String,
    val name: String,
    val memberCount: Int,
    val goalSummary: String,
    val daysRemaining: Int?,
    val inviteCode: String,
)

data class CircleDetailItem(
    val circleId: String,
    val name: String,
    val goal: String,
    val daysRemaining: Int,
    val inviteCode: String,
    val members: kotlin.collections.List<CircleMemberUiItem>,
    val aggregateProgressPercent: Int,
    val isAdmin: Boolean,
)

data class CircleMemberUiItem(
    val userId: String,
    val displayName: String,
    val isCurrentUser: Boolean,
    val fpBalance: Int?,
    val streakDays: Int?,
    val nutritiveMinutes: Int?,
)

/**
 * Circle management screen.
 *
 * Handles:
 *  - List of circles user belongs to
 *  - Circle detail (name, goal, days remaining, members, aggregate progress)
 *  - Create circle flow (name, goal, duration, max members → generates invite code)
 *  - Join circle flow (enter invite code)
 *  - Leave circle option in menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(
    state: CircleDetailUiState = CircleDetailUiState(),
    mode: CircleScreenMode = CircleScreenMode.List,
    onModeChange: (CircleScreenMode) -> Unit = {},
    onCreateCircle: (name: String, goal: String, durationDays: Int, maxMembers: Int) -> Unit = { _, _, _, _ -> },
    onJoinCircle: (inviteCode: String) -> Unit = {},
    onLeaveCircle: (circleId: String) -> Unit = {},
    onCircleTap: (circleId: String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = circleTitle(mode, state), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode is CircleScreenMode.List) onBack() else onModeChange(CircleScreenMode.List)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    CircleTopBarActions(mode = mode, state = state, onLeaveCircle = onLeaveCircle)
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            CircleBody(
                state = state,
                mode = mode,
                callbacks =
                    CircleCallbacks(
                        onModeChange = onModeChange,
                        onCreateCircle = onCreateCircle,
                        onJoinCircle = onJoinCircle,
                        onLeaveCircle = onLeaveCircle,
                        onCircleTap = onCircleTap,
                    ),
            )
        }
    }
}

private fun circleTitle(
    mode: CircleScreenMode,
    state: CircleDetailUiState,
): String =
    when (mode) {
        CircleScreenMode.List -> "Focus Circles"
        is CircleScreenMode.Detail -> state.selectedCircle?.name ?: "Circle"
        CircleScreenMode.Create -> "Create Circle"
        CircleScreenMode.Join -> "Join Circle"
    }

@Composable
private fun CircleTopBarActions(
    mode: CircleScreenMode,
    state: CircleDetailUiState,
    onLeaveCircle: (String) -> Unit,
) {
    if (mode !is CircleScreenMode.Detail || state.selectedCircle?.isAdmin != false) return
    var showMenu by remember { mutableStateOf(false) }
    IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Options")
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(
            text = { Text("Leave Circle", color = MaterialTheme.colorScheme.error) },
            onClick = {
                state.selectedCircle?.let { onLeaveCircle(it.circleId) }
                showMenu = false
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
        )
    }
}

/** Action callbacks for [CircleScreen], grouped to keep composable parameter lists small. */
@Immutable
data class CircleCallbacks(
    val onModeChange: (CircleScreenMode) -> Unit,
    val onCreateCircle: (String, String, Int, Int) -> Unit,
    val onJoinCircle: (String) -> Unit,
    val onLeaveCircle: (String) -> Unit,
    val onCircleTap: (String) -> Unit,
)

@Composable
private fun CircleBody(
    state: CircleDetailUiState,
    mode: CircleScreenMode,
    callbacks: CircleCallbacks,
) {
    when (mode) {
        CircleScreenMode.List ->
            CircleListContent(
                circles = state.circles,
                isLoading = state.isLoading,
                onCreate = { callbacks.onModeChange(CircleScreenMode.Create) },
                onJoin = { callbacks.onModeChange(CircleScreenMode.Join) },
                onCircleTap = {
                    callbacks.onCircleTap(it)
                    callbacks.onModeChange(CircleScreenMode.Detail(it))
                },
            )
        is CircleScreenMode.Detail ->
            CircleDetailOrLoading(
                circle = state.selectedCircle,
                onLeave = callbacks.onLeaveCircle,
            )
        CircleScreenMode.Create -> CreateCircleForm(onCreate = callbacks.onCreateCircle)
        CircleScreenMode.Join -> JoinCircleForm(onJoin = callbacks.onJoinCircle)
    }
}

@Composable
private fun CircleDetailOrLoading(
    circle: CircleDetailItem?,
    onLeave: (String) -> Unit,
) {
    if (circle != null) {
        CircleDetailContent(circle = circle, onLeave = { onLeave(circle.circleId) })
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// ── List content ──────────────────────────────────────────────────────────────

@Composable
private fun CircleListContent(
    circles: kotlin.collections.List<CircleListUiItem>,
    isLoading: Boolean,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onCircleTap: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_GAP_DP.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp)) {
                Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
                    Spacer(Modifier.width(TINY_GAP_DP.dp))
                    Text("Create")
                }
                OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
                    Spacer(Modifier.width(TINY_GAP_DP.dp))
                    Text("Join")
                }
            }
        }

        when {
            isLoading ->
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(LOADING_PAD_DP.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            circles.isEmpty() -> item { EmptyCirclesState() }
            else ->
                items(circles) { circle ->
                    CircleListCard(circle = circle, onClick = { onCircleTap(circle.circleId) })
                }
        }
    }
}

@Composable
private fun EmptyCirclesState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = EMPTY_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LIST_GAP_DP.dp),
    ) {
        Icon(
            Icons.Outlined.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ALPHA_EMPTY_ICON),
            modifier = Modifier.size(EMPTY_ICON_DP.dp),
        )
        Text(
            "No circles yet. Create one or join with an invite code!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CircleListCard(
    circle: CircleListUiItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_PAD_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_CORNER_SM_DP.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(CARD_CORNER_SM_DP.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(AVATAR_LG_DP.dp),
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
                    "${circle.memberCount} members · ${circle.goalSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                circle.daysRemaining?.let {
                    Text(
                        "$it days remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ICON_SIZE_DP.dp),
            )
        }
    }
}

// ── Detail content ────────────────────────────────────────────────────────────

@Composable
private fun CircleDetailContent(
    circle: CircleDetailItem,
    onLeave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp),
    ) {
        item { CircleHeaderCard(circle = circle) }
        item { CircleProgressSection(percent = circle.aggregateProgressPercent) }
        item { InviteCodeCard(inviteCode = circle.inviteCode) }
        item {
            Text(
                "Members (${circle.members.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(circle.members) { member ->
            MemberCard(member = member)
        }
        if (!circle.isAdmin) {
            item {
                OutlinedButton(
                    onClick = onLeave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(ICON_SIZE_DP.dp),
                    )
                    Spacer(Modifier.width(SMALL_GAP_DP.dp))
                    Text("Leave Circle")
                }
            }
        }
        item { Spacer(Modifier.height(SECTION_GAP_DP.dp)) }
    }
}

@Composable
private fun CircleHeaderCard(circle: CircleDetailItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp),
        ) {
            Text(
                circle.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Goal: ${circle.goal}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "${circle.daysRemaining} days remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = ALPHA_SUBTLE),
            )
        }
    }
}

@Composable
private fun CircleProgressSection(percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Group Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { percent / PERCENT_MAX },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PROGRESS_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(PROGRESS_RADIUS_DP.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_TRACK),
        )
    }
}

@Composable
private fun InviteCodeCard(inviteCode: String) {
    Surface(
        shape = RoundedCornerShape(CARD_CORNER_SM_DP.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(CARD_CORNER_SM_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
        ) {
            Icon(
                Icons.Outlined.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ICON_SIZE_DP.dp),
            )
            Column {
                Text(
                    "Invite code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    inviteCode,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MemberCard(member: CircleMemberUiItem) {
    val containerColor =
        if (member.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = ALPHA_CURRENT_USER)
        } else {
            MaterialTheme.colorScheme.surface
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_MD_DP.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (member.isCurrentUser) 0.dp else CARD_ELEVATION_DP.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_PAD_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_CORNER_SM_DP.dp),
        ) {
            MemberAvatar(initial = member.displayName.take(1).uppercase())
            Column(modifier = Modifier.weight(1f)) {
                MemberNameRow(name = member.displayName, isCurrentUser = member.isCurrentUser)
                MemberStats(member = member)
            }
        }
    }
}

@Composable
private fun MemberAvatar(initial: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(AVATAR_SM_DP.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                initial,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun MemberNameRow(
    name: String,
    isCurrentUser: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TINY_GAP_DP.dp),
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (isCurrentUser) {
            Surface(
                shape = RoundedCornerShape(CHIP_CORNER_SM_DP.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_BADGE),
            ) {
                Text(
                    "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = BADGE_PAD_H_DP.dp, vertical = BADGE_PAD_V_DP.dp),
                )
            }
        }
    }
}

@Composable
private fun MemberStats(member: CircleMemberUiItem) {
    val parts =
        buildList {
            member.fpBalance?.let { add("$it FP") }
            member.streakDays?.let { add("${it}d streak") }
            member.nutritiveMinutes?.let { add("${it}m nutritive") }
        }
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
