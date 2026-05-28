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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bilbo.social.ChallengeEngine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// ── Layout / palette constants ────────────────────────────────────────────────
private const val ARGB_COOPERATIVE = 0xFF4CAF50
private const val ARGB_COMPETITIVE = 0xFF2196F3
private const val PERCENT_MAX = 100f

private const val ALPHA_TYPE_TINT = 0.12f
private const val ALPHA_TYPE_TINT_STRONG = 0.2f
private const val ALPHA_TYPE_CARD = 0.10f
private const val ALPHA_MODE_TINT = 0.15f
private const val ALPHA_TRACK = 0.2f
private const val ALPHA_EMPTY_ICON = 0.35f

private const val PADDING_DP = 16
private const val CARD_CORNER_DP = 16
private const val CARD_ELEVATION_DP = 2
private const val LIST_GAP_DP = 12
private const val CARD_PAD_DP = 14
private const val ROW_GAP_DP = 10
private const val SMALL_GAP_DP = 8
private const val TINY_GAP_DP = 6
private const val MICRO_GAP_DP = 4
private const val MICRO_GAP_2_DP = 2
private const val ICON_SIZE_DP = 18
private const val EMPTY_ICON_SIZE_DP = 48
private const val EMPTY_PAD_DP = 40
private const val LOADING_PAD_DP = 32
private const val LIST_PROGRESS_HEIGHT_DP = 6
private const val LIST_PROGRESS_RADIUS_DP = 3
private const val DETAIL_PROGRESS_HEIGHT_DP = 12
private const val DETAIL_PROGRESS_RADIUS_DP = 6
private const val GROUP_PROGRESS_HEIGHT_DP = 14
private const val GROUP_PROGRESS_RADIUS_DP = 7
private const val CHIP_CORNER_DP = 8
private const val CHIP_CORNER_SMALL_DP = 6
private const val BOTTOM_SPACER_DP = 24

// ── UI models ─────────────────────────────────────────────────────────────────

sealed class ChallengeScreenMode {
    data object List : ChallengeScreenMode()

    data class Detail(
        val challengeId: String,
    ) : ChallengeScreenMode()

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
    val leaderboard: kotlin.collections.List<ChallengeLeaderboardEntry>, // competitive
    val groupProgressPercent: Int, // cooperative
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
                    Text(text = challengeTitle(mode, state), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode is ChallengeScreenMode.List) onBack() else onModeChange(ChallengeScreenMode.List)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ChallengeBody(
                state = state,
                mode = mode,
                onModeChange = onModeChange,
                onCreateChallenge = onCreateChallenge,
                onJoinChallenge = onJoinChallenge,
            )
        }
    }
}

private fun challengeTitle(
    mode: ChallengeScreenMode,
    state: ChallengeDetailUiState,
): String =
    when (mode) {
        ChallengeScreenMode.List -> "Challenges"
        is ChallengeScreenMode.Detail -> state.selectedChallenge?.title ?: "Challenge"
        ChallengeScreenMode.Create -> "Create Challenge"
    }

@Composable
private fun ChallengeBody(
    state: ChallengeDetailUiState,
    mode: ChallengeScreenMode,
    onModeChange: (ChallengeScreenMode) -> Unit,
    onCreateChallenge: (String, ChallengeEngine.ChallengeType, Boolean, Int, LocalDate, LocalDate) -> Unit,
    onJoinChallenge: (String) -> Unit,
) {
    when (mode) {
        ChallengeScreenMode.List ->
            ChallengeListContent(
                challenges = state.challenges,
                isLoading = state.isLoading,
                onCreate = { onModeChange(ChallengeScreenMode.Create) },
                onChallengeTap = { id -> onModeChange(ChallengeScreenMode.Detail(id)) },
            )
        is ChallengeScreenMode.Detail ->
            ChallengeDetailOrLoading(
                challenge = state.selectedChallenge,
                onJoin = onJoinChallenge,
            )
        ChallengeScreenMode.Create -> CreateChallengeForm(onCreate = onCreateChallenge)
    }
}

@Composable
private fun ChallengeDetailOrLoading(
    challenge: ChallengeDetailItem?,
    onJoin: (String) -> Unit,
) {
    if (challenge != null) {
        ChallengeDetailContent(
            challenge = challenge,
            onJoin = { onJoin(challenge.challengeId) },
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
        contentPadding = PaddingValues(PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(LIST_GAP_DP.dp),
    ) {
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AddCircle, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
                Spacer(Modifier.width(TINY_GAP_DP.dp))
                Text("Create Challenge")
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
            challenges.isEmpty() -> item { EmptyChallengesState() }
            else ->
                items(challenges) { challenge ->
                    ChallengeListCard(challenge = challenge, onClick = { onChallengeTap(challenge.challengeId) })
                }
        }
    }
}

@Composable
private fun EmptyChallengesState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = EMPTY_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LIST_GAP_DP.dp),
    ) {
        Icon(
            Icons.Outlined.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ALPHA_EMPTY_ICON),
            modifier = Modifier.size(EMPTY_ICON_SIZE_DP.dp),
        )
        Text(
            "No active challenges. Create one to stay motivated!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChallengeListCard(
    challenge: ChallengeListItem,
    onClick: () -> Unit,
) {
    val typeColor = challengeTypeColor(challenge.type)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CARD_PAD_DP.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(challenge.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    ChallengeMetaRow(challenge = challenge, typeColor = typeColor)
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ICON_SIZE_DP.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MICRO_GAP_DP.dp)) {
                LinearProgressIndicator(
                    progress = { challenge.progressPercent / PERCENT_MAX },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(LIST_PROGRESS_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(LIST_PROGRESS_RADIUS_DP.dp)),
                    color = typeColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_TRACK),
                )
                Text(
                    "${challenge.progressPercent}% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChallengeMetaRow(
    challenge: ChallengeListItem,
    typeColor: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TINY_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(CHIP_CORNER_SMALL_DP.dp), color = typeColor.copy(alpha = ALPHA_TYPE_TINT)) {
            Text(
                challenge.type.displayName(),
                style = MaterialTheme.typography.labelSmall,
                color = typeColor,
                modifier = Modifier.padding(horizontal = TINY_GAP_DP.dp, vertical = MICRO_GAP_2_DP.dp),
            )
        }
        MetaDot()
        Text(
            if (challenge.isTeam) "Team" else "Solo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MetaDot()
        Text(
            "${challenge.daysRemaining}d left",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetaDot() {
    Text(
        "·",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Detail content ────────────────────────────────────────────────────────────

@Composable
private fun ChallengeDetailContent(
    challenge: ChallengeDetailItem,
    onJoin: () -> Unit,
) {
    val isCompetitive = !challenge.isTeam
    val typeColor = challengeTypeColor(challenge.type)
    val canJoin = challenge.status == ChallengeEngine.ChallengeStatus.UPCOMING

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(PADDING_DP.dp),
    ) {
        item { ChallengeMetaCard(challenge = challenge, typeColor = typeColor) }
        item { ChallengeProgressCard(challenge = challenge, typeColor = typeColor) }

        if (isCompetitive) {
            leaderboardSection(challenge.leaderboard)
        } else {
            item { GroupProgressSection(groupProgressPercent = challenge.groupProgressPercent) }
        }

        if (canJoin) {
            item { JoinChallengeButton(onJoin = onJoin) }
        }

        item { Spacer(Modifier.height(BOTTOM_SPACER_DP.dp)) }
    }
}

@Composable
private fun JoinChallengeButton(onJoin: () -> Unit) {
    Button(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
        Spacer(Modifier.width(SMALL_GAP_DP.dp))
        Text("Join Challenge")
    }
}

@Composable
private fun ChallengeMetaCard(
    challenge: ChallengeDetailItem,
    typeColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = ALPHA_TYPE_CARD)),
    ) {
        Column(
            modifier = Modifier.padding(PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp),
        ) {
            MetaBadgeRow(challenge = challenge, typeColor = typeColor)
            if (challenge.description.isNotBlank()) {
                Text(
                    challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetaStat(label = "Start", value = challenge.startDate.toString())
                MetaStat(label = "End", value = challenge.endDate.toString())
                MetaStat(label = "Days Left", value = "${challenge.daysRemaining}", valueColor = typeColor)
            }
        }
    }
}

@Composable
private fun MetaBadgeRow(
    challenge: ChallengeDetailItem,
    typeColor: Color,
) {
    val modeColor = if (challenge.isTeam) Color(ARGB_COOPERATIVE) else Color(ARGB_COMPETITIVE)
    val modeText = if (challenge.isTeam) "Cooperative" else "Competitive"
    Row(
        horizontalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetaBadge(text = challenge.type.displayName(), color = typeColor, tint = ALPHA_TYPE_TINT_STRONG)
        MetaBadge(text = modeText, color = modeColor, tint = ALPHA_MODE_TINT)
    }
}

@Composable
private fun MetaBadge(
    text: String,
    color: Color,
    tint: Float,
) {
    Surface(shape = RoundedCornerShape(CHIP_CORNER_DP.dp), color = color.copy(alpha = tint)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = SMALL_GAP_DP.dp, vertical = MICRO_GAP_DP.dp),
        )
    }
}

@Composable
private fun MetaStat(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor == Color.Unspecified) LocalDefaultTextColor() else valueColor,
        )
    }
}

@Composable
private fun LocalDefaultTextColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
private fun ChallengeProgressCard(
    challenge: ChallengeDetailItem,
    typeColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(PADDING_DP.dp), verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp)) {
            Text("Your Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { challenge.myProgressPercent / PERCENT_MAX },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(DETAIL_PROGRESS_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(DETAIL_PROGRESS_RADIUS_DP.dp)),
                color = typeColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_TRACK),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${challenge.myProgress} / ${challenge.targetValue}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${challenge.myProgressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = typeColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GroupProgressSection(groupProgressPercent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
        Text("Group Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress = { groupProgressPercent / PERCENT_MAX },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(GROUP_PROGRESS_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(GROUP_PROGRESS_RADIUS_DP.dp)),
            color = Color(ARGB_COOPERATIVE),
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_TRACK),
        )
        Text(
            "$groupProgressPercent% of team goal reached",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


// ── Create challenge form ─────────────────────────────────────────────────────

