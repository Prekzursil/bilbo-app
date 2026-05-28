package dev.bilbo.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bilbo.social.BuddyManager

// ── Layout / limit constants ──────────────────────────────────────────────────
private const val ARGB_ONLINE = 0xFF4CAF50
private const val INVITE_CODE_LEN = 6
private const val ENCOURAGEMENT_MAX_LEN = 100
private const val ENCOURAGEMENT_MAX_LINES = 3
private const val CODE_LETTER_SPACING = 4f
private const val INVITE_LETTER_SPACING = 6f
private const val PADDING_DP = 16
private const val SECTION_GAP_DP = 16
private const val SMALL_GAP_DP = 8
private const val TINY_GAP_DP = 6
private const val ROW_GAP_DP = 10
private const val CARD_GAP_DP = 12
private const val MICRO_GAP_DP = 4
private const val ICON_SIZE_DP = 18
private const val SMALL_ICON_DP = 16
private const val AVATAR_DP = 48
private const val ONLINE_DOT_DP = 12
private const val UNREAD_DOT_DP = 8
private const val CLOSE_BTN_DP = 24
private const val EMPTY_ICON_DP = 48
private const val EMPTY_PAD_DP = 32
private const val NOTICE_PAD_DP = 10
private const val INVITE_PAD_DP = 20
private const val CARD_CORNER_DP = 16
private const val CARD_CORNER_MD_DP = 14
private const val CARD_CORNER_SM_DP = 12
private const val NOTICE_CORNER_DP = 10
private const val CARD_ELEVATION_DP = 2
private const val PROGRESS_STROKE_DP = 2
private const val ALPHA_EMPTY_ICON = 0.35f
private const val ALPHA_DIVIDER = 0.2f
private const val ALPHA_UNREAD = 0.5f
private const val ENC_BTN_PAD_V_DP = 8
private const val BOTTOM_SPACER_DP = 24

// ── UI models ─────────────────────────────────────────────────────────────────

data class BuddyPairDetailUiState(
    val pairs: List<BuddyPairDetailItem> = emptyList(),
    val nudges: List<NudgeItem> = emptyList(),
    val generatedInviteCode: String? = null,
    val isGeneratingCode: Boolean = false,
    val isLoading: Boolean = false,
)

data class BuddyPairDetailItem(
    val pairId: String,
    val buddyDisplayName: String,
    val sharingLevel: BuddyManager.SharingLevel,
    val statusSummary: String, // e.g. "312 FP today · 5-day streak"
    val isOnline: Boolean = false,
)

data class NudgeItem(
    val nudgeId: String,
    val fromName: String,
    val message: String,
    val timeAgo: String,
    val isUnread: Boolean,
)

/**
 * Buddy pair management screen.
 *
 * Features:
 *  - List of current buddy pairs (max 3) with sharing-level selector
 *  - "Invite a Buddy" → generates 6-char code + share dialog
 *  - "Enter Code" → input dialog to accept invite
 *  - Nudge inbox with unread highlighting
 *  - Send encouragement (100-char limit)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyPairScreen(
    state: BuddyPairDetailUiState = BuddyPairDetailUiState(),
    onInviteBuddy: () -> Unit = {},
    onEnterCode: (String) -> Unit = {},
    onSharingLevelChange: (pairId: String, level: BuddyManager.SharingLevel) -> Unit = { _, _ -> },
    onRemovePair: (String) -> Unit = {},
    onSendEncouragement: (pairId: String, message: String) -> Unit = { _, _ -> },
    onDismissNudge: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    var showEnterCodeDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var encouragementTarget by remember { mutableStateOf<String?>(null) }
    var encouragementText by remember { mutableStateOf("") }

    // Show invite dialog when code is generated
    LaunchedEffect(state.generatedInviteCode) {
        if (state.generatedInviteCode != null) showInviteDialog = true
    }

    val callbacks =
        BuddyPairCallbacks(
            onInviteBuddy = onInviteBuddy,
            onShowEnterCode = { showEnterCodeDialog = true },
            onSharingLevelChange = onSharingLevelChange,
            onRemovePair = onRemovePair,
            onStartEncouragement = { pairId ->
                encouragementTarget = pairId
                encouragementText = ""
            },
            onDismissNudge = onDismissNudge,
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accountability Buddies", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        BuddyPairList(
            state = state,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            callbacks = callbacks,
        )
    }

    if (showEnterCodeDialog) {
        EnterCodeDialog(onJoin = onEnterCode, onDismiss = { showEnterCodeDialog = false })
    }
    if (showInviteDialog && state.generatedInviteCode != null) {
        InviteCodeDialog(inviteCode = state.generatedInviteCode, onDismiss = { showInviteDialog = false })
    }
    val target = encouragementTarget
    if (target != null) {
        EncouragementDialog(
            targetName = state.pairs.find { it.pairId == target }?.buddyDisplayName,
            text = encouragementText,
            onTextChange = { encouragementText = it },
            onSend = {
                onSendEncouragement(target, encouragementText)
                encouragementTarget = null
            },
            onDismiss = { encouragementTarget = null },
        )
    }
}

/** Action callbacks for [BuddyPairList], grouped to keep parameter lists small. */
@Immutable
data class BuddyPairCallbacks(
    val onInviteBuddy: () -> Unit,
    val onShowEnterCode: () -> Unit,
    val onSharingLevelChange: (String, BuddyManager.SharingLevel) -> Unit,
    val onRemovePair: (String) -> Unit,
    val onStartEncouragement: (String) -> Unit,
    val onDismissNudge: (String) -> Unit,
)

@Composable
private fun BuddyPairList(
    state: BuddyPairDetailUiState,
    modifier: Modifier,
    callbacks: BuddyPairCallbacks,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp),
    ) {
        item {
            BuddyActionButtons(
                isGeneratingCode = state.isGeneratingCode,
                canInvite = state.pairs.size < BuddyManager.MAX_BUDDY_PAIRS,
                onInviteBuddy = callbacks.onInviteBuddy,
                onShowEnterCode = callbacks.onShowEnterCode,
            )
        }

        if (state.pairs.size >= BuddyManager.MAX_BUDDY_PAIRS) {
            item { MaxPairsNotice() }
        }

        if (state.pairs.isEmpty()) {
            item { EmptyBuddyState() }
        } else {
            item {
                Text("Your Buddies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(state.pairs) { pair ->
                BuddyPairCard(
                    pair = pair,
                    onSharingLevelChange = { level -> callbacks.onSharingLevelChange(pair.pairId, level) },
                    onRemove = { callbacks.onRemovePair(pair.pairId) },
                    onSendEncouragement = { callbacks.onStartEncouragement(pair.pairId) },
                )
            }
        }

        if (state.nudges.isNotEmpty()) {
            item {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = ALPHA_DIVIDER))
                Spacer(Modifier.height(MICRO_GAP_DP.dp))
                Text("Nudges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(state.nudges) { nudge ->
                NudgeCard(nudge = nudge, onDismiss = { callbacks.onDismissNudge(nudge.nudgeId) })
            }
        }

        item { Spacer(Modifier.height(BOTTOM_SPACER_DP.dp)) }
    }
}

@Composable
private fun BuddyActionButtons(
    isGeneratingCode: Boolean,
    canInvite: Boolean,
    onInviteBuddy: () -> Unit,
    onShowEnterCode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp),
    ) {
        Button(
            onClick = onInviteBuddy,
            modifier = Modifier.weight(1f),
            enabled = canInvite && !isGeneratingCode,
        ) {
            if (isGeneratingCode) {
                CircularProgressIndicator(Modifier.size(SMALL_ICON_DP.dp), strokeWidth = PROGRESS_STROKE_DP.dp)
            } else {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
            }
            Spacer(Modifier.width(TINY_GAP_DP.dp))
            Text("Invite a Buddy")
        }
        OutlinedButton(onClick = onShowEnterCode, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
            Spacer(Modifier.width(TINY_GAP_DP.dp))
            Text("Enter Code")
        }
    }
}

@Composable
private fun MaxPairsNotice() {
    Surface(
        shape = RoundedCornerShape(NOTICE_CORNER_DP.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "Maximum ${BuddyManager.MAX_BUDDY_PAIRS} buddy pairs reached.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(NOTICE_PAD_DP.dp),
        )
    }
}

@Composable
private fun EmptyBuddyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = EMPTY_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CARD_GAP_DP.dp),
    ) {
        Icon(
            Icons.Outlined.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ALPHA_EMPTY_ICON),
            modifier = Modifier.size(EMPTY_ICON_DP.dp),
        )
        Text(
            "Invite a friend to become accountability buddies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun EnterCodeDialog(
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Invite Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
                Text(
                    "Ask your buddy for their $INVITE_CODE_LEN-character code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= INVITE_CODE_LEN) codeInput = it.uppercase() },
                    placeholder = { Text("XXXXXX") },
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing =
                                androidx.compose.ui.unit
                                    .TextUnit(CODE_LETTER_SPACING, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onJoin(codeInput)
                    onDismiss()
                },
                enabled = codeInput.length == INVITE_CODE_LEN,
            ) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun InviteCodeDialog(
    inviteCode: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Invite Code") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(CARD_GAP_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Share this code with your buddy. It expires in 48 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(CARD_CORNER_SM_DP.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = inviteCode,
                        style =
                            MaterialTheme.typography.displaySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing =
                                    androidx.compose.ui.unit
                                        .TextUnit(INVITE_LETTER_SPACING, androidx.compose.ui.unit.TextUnitType.Sp),
                            ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(INVITE_PAD_DP.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun EncouragementDialog(
    targetName: String?,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Encouragement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP_DP.dp)) {
                if (targetName != null) {
                    Text(
                        "To: $targetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= ENCOURAGEMENT_MAX_LEN) onTextChange(it) },
                    placeholder = { Text("Write a short message…") },
                    maxLines = ENCOURAGEMENT_MAX_LINES,
                    supportingText = { Text("${text.length}/$ENCOURAGEMENT_MAX_LEN") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSend, enabled = text.isNotBlank()) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun BuddyPairCard(
    pair: BuddyPairDetailItem,
    onSharingLevelChange: (BuddyManager.SharingLevel) -> Unit,
    onRemove: () -> Unit,
    onSendEncouragement: () -> Unit,
) {
    var showLevelPicker by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_DP.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(CARD_GAP_DP.dp),
        ) {
            BuddyCardHeader(pair = pair, onOptions = { showRemoveConfirm = true })
            BuddySharingRow(level = pair.sharingLevel, onClick = { showLevelPicker = true })
            OutlinedButton(
                onClick = onSendEncouragement,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = ENC_BTN_PAD_V_DP.dp),
            ) {
                Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(SMALL_ICON_DP.dp))
                Spacer(Modifier.width(TINY_GAP_DP.dp))
                Text("Send encouragement", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showLevelPicker) {
        SharingLevelDialog(
            current = pair.sharingLevel,
            onSelect = {
                onSharingLevelChange(it)
                showLevelPicker = false
            },
            onDismiss = { showLevelPicker = false },
        )
    }

    if (showRemoveConfirm) {
        RemoveBuddyDialog(
            buddyName = pair.buddyDisplayName,
            onRemove = {
                onRemove()
                showRemoveConfirm = false
            },
            onDismiss = { showRemoveConfirm = false },
        )
    }
}

@Composable
private fun BuddyCardHeader(
    pair: BuddyPairDetailItem,
    onOptions: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP_DP.dp),
    ) {
        Box {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(AVATAR_DP.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        pair.buddyDisplayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (pair.isOnline) {
                Box(
                    modifier =
                        Modifier
                            .size(ONLINE_DOT_DP.dp)
                            .clip(CircleShape)
                            .background(Color(ARGB_ONLINE))
                            .align(Alignment.BottomEnd),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(pair.buddyDisplayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                pair.statusSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onOptions) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
    }
}

@Composable
private fun BuddySharingRow(
    level: BuddyManager.SharingLevel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Sharing level:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick) {
            Text(
                level.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(ICON_SIZE_DP.dp))
        }
    }
}

private fun sharingLevelDescription(level: BuddyManager.SharingLevel): String =
    when (level) {
        BuddyManager.SharingLevel.MINIMAL -> "Presence only — buddy knows you're here"
        BuddyManager.SharingLevel.BASIC -> "FP balance and streak"
        BuddyManager.SharingLevel.STANDARD -> "Daily FP summary"
        BuddyManager.SharingLevel.DETAILED -> "Full breakdown including app categories"
    }

@Composable
private fun SharingLevelDialog(
    current: BuddyManager.SharingLevel,
    onSelect: (BuddyManager.SharingLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sharing Level") },
        text = {
            Column {
                BuddyManager.SharingLevel.entries.forEach { level ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == level, onClick = { onSelect(level) })
                        Column {
                            Text(
                                level.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                sharingLevelDescription(level),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun RemoveBuddyDialog(
    buddyName: String,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Buddy?") },
        text = { Text("Are you sure you want to remove $buddyName as a buddy?") },
        confirmButton = {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NudgeCard(
    nudge: NudgeItem,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(CARD_CORNER_MD_DP.dp),
        color =
            if (nudge.isUnread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = ALPHA_UNREAD)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(CARD_CORNER_MD_DP.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(CARD_GAP_DP.dp),
        ) {
            if (nudge.isUnread) {
                Box(
                    modifier =
                        Modifier
                            .size(UNREAD_DOT_DP.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(top = TINY_GAP_DP.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MICRO_GAP_DP.dp)) {
                Text(
                    text = nudge.fromName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(nudge.message, style = MaterialTheme.typography.bodySmall)
                Text(
                    nudge.timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(CLOSE_BTN_DP.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(SMALL_ICON_DP.dp))
            }
        }
    }
}
