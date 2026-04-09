package dev.spark.app.ui.screen

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
import androidx.compose.ui.unit.dp
import dev.spark.social.BuddyManager

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
    val statusSummary: String,         // e.g. "312 FP today · 5-day streak"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onInviteBuddy,
                        modifier = Modifier.weight(1f),
                        enabled = state.pairs.size < BuddyManager.MAX_BUDDY_PAIRS && !state.isGeneratingCode,
                    ) {
                        if (state.isGeneratingCode) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Invite a Buddy")
                    }
                    OutlinedButton(
                        onClick = { showEnterCodeDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enter Code")
                    }
                }
            }

            // Max pairs notice
            if (state.pairs.size >= BuddyManager.MAX_BUDDY_PAIRS) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "Maximum ${BuddyManager.MAX_BUDDY_PAIRS} buddy pairs reached.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }

            // Buddy pair cards
            if (state.pairs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            "Invite a friend to become accountability buddies.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                item {
                    Text("Your Buddies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                items(state.pairs) { pair ->
                    BuddyPairCard(
                        pair = pair,
                        onSharingLevelChange = { level -> onSharingLevelChange(pair.pairId, level) },
                        onRemove = { onRemovePair(pair.pairId) },
                        onSendEncouragement = { encouragementTarget = pair.pairId; encouragementText = "" },
                    )
                }
            }

            // Nudge inbox
            if (state.nudges.isNotEmpty()) {
                item {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))
                    Text("Nudges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                items(state.nudges) { nudge ->
                    NudgeCard(nudge = nudge, onDismiss = { onDismissNudge(nudge.nudgeId) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Enter code dialog ──────────────────────────────────────────────────────
    if (showEnterCodeDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEnterCodeDialog = false },
            title = { Text("Enter Invite Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ask your buddy for their 6-character code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6) codeInput = it.uppercase() },
                        placeholder = { Text("XXXXXX") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp),
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEnterCode(codeInput)
                        showEnterCodeDialog = false
                    },
                    enabled = codeInput.length == 6,
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showEnterCodeDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Invite code share dialog ──────────────────────────────────────────────
    if (showInviteDialog && state.generatedInviteCode != null) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Your Invite Code") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Share this code with your buddy. It expires in 48 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = state.generatedInviteCode,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                letterSpacing = androidx.compose.ui.unit.TextUnit(6f, androidx.compose.ui.unit.TextUnitType.Sp),
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Done") }
            },
        )
    }

    // ── Encouragement dialog ──────────────────────────────────────────────────
    val currentTarget = encouragementTarget
    if (currentTarget != null) {
        val targetPair = state.pairs.find { it.pairId == currentTarget }
        AlertDialog(
            onDismissRequest = { encouragementTarget = null },
            title = { Text("Send Encouragement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (targetPair != null) {
                        Text(
                            "To: ${targetPair.buddyDisplayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = encouragementText,
                        onValueChange = { if (it.length <= 100) encouragementText = it },
                        placeholder = { Text("Write a short message…") },
                        maxLines = 3,
                        supportingText = { Text("${encouragementText.length}/100") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSendEncouragement(currentTarget, encouragementText)
                        encouragementTarget = null
                    },
                    enabled = encouragementText.isNotBlank(),
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { encouragementTarget = null }) { Text("Cancel") }
            },
        )
    }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp),
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
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .align(Alignment.BottomEnd),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(pair.buddyDisplayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(pair.statusSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(onClick = { showRemoveConfirm = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }

            // Sharing level row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sharing level:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showLevelPicker = true }) {
                    Text(
                        pair.sharingLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            // Send encouragement button
            OutlinedButton(
                onClick = onSendEncouragement,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send encouragement", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    // Sharing level picker dropdown
    if (showLevelPicker) {
        AlertDialog(
            onDismissRequest = { showLevelPicker = false },
            title = { Text("Sharing Level") },
            text = {
                Column {
                    BuddyManager.SharingLevel.entries.forEach { level ->
                        val description = when (level) {
                            BuddyManager.SharingLevel.MINIMAL  -> "Presence only — buddy knows you're here"
                            BuddyManager.SharingLevel.BASIC    -> "FP balance and streak"
                            BuddyManager.SharingLevel.STANDARD -> "Daily FP summary"
                            BuddyManager.SharingLevel.DETAILED -> "Full breakdown including app categories"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = pair.sharingLevel == level,
                                onClick = {
                                    onSharingLevelChange(level)
                                    showLevelPicker = false
                                },
                            )
                            Column {
                                Text(level.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelPicker = false }) { Text("Close") }
            },
        )
    }

    // Remove confirmation
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove Buddy?") },
            text = { Text("Are you sure you want to remove ${pair.buddyDisplayName} as a buddy?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun NudgeCard(nudge: NudgeItem, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (nudge.isUnread)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (nudge.isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(top = 6.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = nudge.fromName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(nudge.message, style = MaterialTheme.typography.bodySmall)
                Text(nudge.timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}
