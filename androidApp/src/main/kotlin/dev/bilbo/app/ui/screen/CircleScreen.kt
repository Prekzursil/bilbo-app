package dev.bilbo.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bilbo.social.CircleManager

// ── UI models ─────────────────────────────────────────────────────────────────

sealed class CircleScreenMode {
    /** Show the list of user's circles. */
    data object List : CircleScreenMode()
    /** Detail view for a specific circle. */
    data class Detail(val circleId: String) : CircleScreenMode()
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
                    Text(
                        text = when (mode) {
                            CircleScreenMode.List   -> "Focus Circles"
                            is CircleScreenMode.Detail -> state.selectedCircle?.name ?: "Circle"
                            CircleScreenMode.Create -> "Create Circle"
                            CircleScreenMode.Join   -> "Join Circle"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (mode) {
                            CircleScreenMode.List -> onBack()
                            else -> onModeChange(CircleScreenMode.List)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    if (mode is CircleScreenMode.Detail && state.selectedCircle?.isAdmin == false) {
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
                                    Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (mode) {
                CircleScreenMode.List -> CircleListContent(
                    circles = state.circles,
                    isLoading = state.isLoading,
                    onCreate = { onModeChange(CircleScreenMode.Create) },
                    onJoin = { onModeChange(CircleScreenMode.Join) },
                    onCircleTap = { onCircleTap(it); onModeChange(CircleScreenMode.Detail(it)) },
                )
                is CircleScreenMode.Detail -> {
                    val circle = state.selectedCircle
                    if (circle != null) {
                        CircleDetailContent(
                            circle = circle,
                            onLeave = { onLeaveCircle(circle.circleId) },
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                CircleScreenMode.Create -> CreateCircleForm(onCreate = onCreateCircle)
                CircleScreenMode.Join -> JoinCircleForm(onJoin = onJoinCircle)
            }
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create")
                }
                OutlinedButton(onClick = onJoin, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Join")
                }
            }
        }

        if (isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (circles.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(48.dp))
                    Text("No circles yet. Create one or join with an invite code!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(circles) { circle ->
                CircleListCard(circle = circle, onClick = { onCircleTap(circle.circleId) })
            }
        }
    }
}

@Composable
private fun CircleListCard(circle: CircleListUiItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(circle.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${circle.memberCount} members · ${circle.goalSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                circle.daysRemaining?.let {
                    Text("$it days remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(circle.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Goal: ${circle.goal}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${circle.daysRemaining} days remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        // Aggregate progress
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Group Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${circle.aggregateProgressPercent}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { circle.aggregateProgressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        }

        // Invite code
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Column {
                        Text("Invite code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(circle.inviteCode, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Members section
        item {
            Text("Members (${circle.members.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        items(circle.members) { member ->
            MemberCard(member = member)
        }

        // Leave circle (for non-admins — admins use menu)
        item { Spacer(Modifier.height(8.dp)) }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MemberCard(member: CircleMemberUiItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (member.isCurrentUser) 0.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.displayName.take(1).uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(member.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (member.isCurrentUser) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text("You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                // Stats visible per sharing level
                val parts = buildList {
                    member.fpBalance?.let { add("$it FP") }
                    member.streakDays?.let { add("${it}d streak") }
                    member.nutritiveMinutes?.let { add("${it}m nutritive") }
                }
                if (parts.isNotEmpty()) {
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Create circle form ────────────────────────────────────────────────────────

@Composable
private fun CreateCircleForm(
    onCreate: (name: String, goal: String, durationDays: Int, maxMembers: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(14) }
    var maxMembers by remember { mutableStateOf(5) }

    val durationOptions = listOf(7, 14, 30)
    val maxMemberOptions = (3..7).toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Create a Focus Circle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Invite friends to hold each other accountable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

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
                maxLines = 2,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durationOptions.forEach { days ->
                        FilterChip(
                            selected = duration == days,
                            onClick = { duration = days },
                            label = { Text("${days}d") },
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Max Members", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    maxMemberOptions.forEach { count ->
                        FilterChip(
                            selected = maxMembers == count,
                            onClick = { maxMembers = count },
                            label = { Text("$count") },
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, goal, duration, maxMembers) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Circle")
            }
        }
    }
}

// ── Join circle form ──────────────────────────────────────────────────────────

@Composable
private fun JoinCircleForm(onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Link,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            "Enter Invite Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            "Ask the circle admin for their 8-character invite code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 8) code = it.uppercase() },
            placeholder = { Text("XXXXXXXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp),
            ),
        )

        Button(
            onClick = { if (code.length == 8) onJoin(code) },
            modifier = Modifier.fillMaxWidth(),
            enabled = code.length == 8,
        ) {
            Text("Join Circle")
        }
    }
}
