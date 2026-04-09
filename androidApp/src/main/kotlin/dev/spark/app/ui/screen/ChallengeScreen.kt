package dev.spark.app.ui.screen

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
import dev.spark.social.ChallengeEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// ── UI models ─────────────────────────────────────────────────────────────────

sealed class ChallengeScreenMode {
    data object List : ChallengeScreenMode()
    data class Detail(val challengeId: String) : ChallengeScreenMode()
    data object Create : ChallengeScreenMode()
}

data class ChallengeDetailUiState(
    val challenges: kotlin.collections.List<ChallengeListItem> = emptyList(),
    val selectedChallenge: ChallengeDetailItem? = null,
    val isLoading: Boolean = false,
)

data class ChallengeListItem(
    val challengeId: String,
    val title: String,
    val type: ChallengeEngine.ChallengeType,
    val isTeam: Boolean,
    val progressPercent: Int,
    val daysRemaining: Int,
    val status: ChallengeEngine.ChallengeStatus,
)

data class ChallengeDetailItem(
    val challengeId: String,
    val title: String,
    val description: String,
    val type: ChallengeEngine.ChallengeType,
    val scope: ChallengeEngine.ChallengeScope,
    val isTeam: Boolean,
    val targetValue: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysRemaining: Int,
    val status: ChallengeEngine.ChallengeStatus,
    val myProgress: Int,
    val myProgressPercent: Int,
    val leaderboard: kotlin.collections.List<ChallengeLeaderboardEntry>,       // competitive
    val groupProgressPercent: Int,                                              // cooperative
)

data class ChallengeLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val progress: Int,
    val progressLabel: String,
    val isCurrentUser: Boolean,
)

/**
 * Challenge management screen.
 *
 * Features:
 *  - List of challenges (active + upcoming)
 *  - Challenge detail with progress tracker
 *  - Competitive: mini leaderboard
 *  - Cooperative: group progress bar
 *  - Create challenge form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    state: ChallengeDetailUiState = ChallengeDetailUiState(),
    mode: ChallengeScreenMode = ChallengeScreenMode.List,
    onModeChange: (ChallengeScreenMode) -> Unit = {},
    onCreateChallenge: (
        title: String,
        type: ChallengeEngine.ChallengeType,
        isTeam: Boolean,
        targetValue: Int,
        startDate: LocalDate,
        endDate: LocalDate,
    ) -> Unit = { _, _, _, _, _, _ -> },
    onJoinChallenge: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (mode) {
                            ChallengeScreenMode.List   -> "Challenges"
                            is ChallengeScreenMode.Detail -> state.selectedChallenge?.title ?: "Challenge"
                            ChallengeScreenMode.Create -> "Create Challenge"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (mode) {
                            ChallengeScreenMode.List -> onBack()
                            else -> onModeChange(ChallengeScreenMode.List)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (mode) {
                ChallengeScreenMode.List -> ChallengeListContent(
                    challenges = state.challenges,
                    isLoading = state.isLoading,
                    onCreate = { onModeChange(ChallengeScreenMode.Create) },
                    onChallengeTap = { id -> onModeChange(ChallengeScreenMode.Detail(id)) },
                )
                is ChallengeScreenMode.Detail -> {
                    val challenge = state.selectedChallenge
                    if (challenge != null) {
                        ChallengeDetailContent(challenge = challenge, onJoin = { onJoinChallenge(challenge.challengeId) })
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                }
                ChallengeScreenMode.Create -> CreateChallengeForm(onCreate = onCreateChallenge)
            }
        }
    }
}

// ── List content ──────────────────────────────────────────────────────────────

@Composable
private fun ChallengeListContent(
    challenges: kotlin.collections.List<ChallengeListItem>,
    isLoading: Boolean,
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

        if (isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (challenges.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(48.dp))
                    Text("No active challenges. Create one to stay motivated!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(challenges) { challenge ->
                ChallengeListCard(challenge = challenge, onClick = { onChallengeTap(challenge.challengeId) })
            }
        }
    }
}

@Composable
private fun ChallengeListCard(challenge: ChallengeListItem, onClick: () -> Unit) {
    val typeColor = challengeTypeColor(challenge.type)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = typeColor.copy(alpha = 0.12f)) {
                            Text(challenge.type.displayName(), style = MaterialTheme.typography.labelSmall, color = typeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (challenge.isTeam) "Team" else "Solo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${challenge.daysRemaining}d left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { challenge.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = typeColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Text("${challenge.progressPercent}% complete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Detail content ────────────────────────────────────────────────────────────

@Composable
private fun ChallengeDetailContent(challenge: ChallengeDetailItem, onJoin: () -> Unit) {
    val isCompetitive = !challenge.isTeam
    val typeColor = challengeTypeColor(challenge.type)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Meta card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.10f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.2f)) {
                            Text(challenge.type.displayName(), style = MaterialTheme.typography.labelSmall, color = typeColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = if (challenge.isTeam) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF2196F3).copy(alpha = 0.15f)) {
                            Text(if (challenge.isTeam) "Cooperative" else "Competitive", style = MaterialTheme.typography.labelSmall, color = if (challenge.isTeam) Color(0xFF4CAF50) else Color(0xFF2196F3), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    if (challenge.description.isNotBlank()) {
                        Text(challenge.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(challenge.startDate.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(challenge.endDate.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Days Left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${challenge.daysRemaining}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = typeColor)
                        }
                    }
                }
            }
        }

        // My progress tracker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { challenge.myProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color = typeColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${challenge.myProgress} / ${challenge.targetValue}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("${challenge.myProgressPercent}%", style = MaterialTheme.typography.bodySmall, color = typeColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Competitive: mini leaderboard
        if (isCompetitive && challenge.leaderboard.isNotEmpty()) {
            item {
                Text("Leaderboard", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(challenge.leaderboard.take(5)) { entry ->
                ChallengeLeaderboardRow(entry = entry)
            }
        }

        // Cooperative: group progress
        if (!isCompetitive) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Group Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { challenge.groupProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    Text(
                        "${challenge.groupProgressPercent}% of team goal reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ChallengeLeaderboardRow(entry: ChallengeLeaderboardEntry) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rankEmoji = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (entry.isCurrentUser)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = if (entry.isCurrentUser) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(rankEmoji, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
            Text(entry.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
            Text(entry.progressLabel, style = MaterialTheme.typography.bodySmall, color = rankColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Create challenge form ─────────────────────────────────────────────────────

@Composable
private fun CreateChallengeForm(
    onCreate: (String, ChallengeEngine.ChallengeType, Boolean, Int, LocalDate, LocalDate) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES) }
    var isTeam by remember { mutableStateOf(false) }
    var targetValue by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf(7) }

    val durationOptions = listOf(3, 7, 14, 30)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Challenge title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                val types = ChallengeEngine.ChallengeType.entries
                types.forEach { type ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                        Column {
                            Text(type.displayName(), style = MaterialTheme.typography.bodyMedium)
                            Text(type.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isTeam, onClick = { isTeam = false }, label = { Text("Competitive") })
                    FilterChip(selected = isTeam, onClick = { isTeam = true }, label = { Text("Cooperative") })
                }
            }
        }

        item {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { if (it.all(Char::isDigit)) targetValue = it },
                label = { Text("Target value (${selectedType.unit()})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durationOptions.forEach { days ->
                        FilterChip(selected = durationDays == days, onClick = { durationDays = days }, label = { Text("${days}d") })
                    }
                }
            }
        }

        item {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val endDate = today.plus(DatePeriod(days = durationDays))

            Button(
                onClick = {
                    val target = targetValue.toIntOrNull() ?: return@Button
                    if (title.isNotBlank() && target > 0) {
                        onCreate(title, selectedType, isTeam, target, today, endDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && targetValue.toIntOrNull() != null,
            ) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Challenge")
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun ChallengeEngine.ChallengeType.displayName(): String = when (this) {
    ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES  -> "Reduce Scrolling"
    ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "Earn Nutritive Time"
    ChallengeEngine.ChallengeType.REACH_FP_BALANCE       -> "Reach FP Balance"
    ChallengeEngine.ChallengeType.DAILY_STREAK           -> "Daily Streak"
    ChallengeEngine.ChallengeType.GROUP_FP_POOL          -> "Group FP Pool"
    ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS     -> "Analog Activities"
}

private fun ChallengeEngine.ChallengeType.description(): String = when (this) {
    ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES  -> "Reduce empty-calorie minutes below a target"
    ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "Accumulate nutritive screen time"
    ChallengeEngine.ChallengeType.REACH_FP_BALANCE       -> "Reach a Focus Points balance"
    ChallengeEngine.ChallengeType.DAILY_STREAK           -> "Maintain a consecutive-day streak"
    ChallengeEngine.ChallengeType.GROUP_FP_POOL          -> "Collectively earn Focus Points"
    ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS     -> "Complete analog activity suggestions"
}

private fun ChallengeEngine.ChallengeType.unit(): String = when (this) {
    ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES  -> "max minutes"
    ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "minutes"
    ChallengeEngine.ChallengeType.REACH_FP_BALANCE       -> "FP"
    ChallengeEngine.ChallengeType.DAILY_STREAK           -> "days"
    ChallengeEngine.ChallengeType.GROUP_FP_POOL          -> "FP total"
    ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS     -> "activities"
}

@Composable
private fun challengeTypeColor(type: ChallengeEngine.ChallengeType): Color = when (type) {
    ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES  -> Color(0xFFF44336)
    ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> Color(0xFF4CAF50)
    ChallengeEngine.ChallengeType.REACH_FP_BALANCE       -> Color(0xFFFF9800)
    ChallengeEngine.ChallengeType.DAILY_STREAK           -> Color(0xFF9C27B0)
    ChallengeEngine.ChallengeType.GROUP_FP_POOL          -> Color(0xFF2196F3)
    ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS     -> Color(0xFF009688)
}
