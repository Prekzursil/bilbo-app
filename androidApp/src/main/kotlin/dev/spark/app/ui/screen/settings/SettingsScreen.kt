// SettingsScreen.kt
// Bilbo — Android Settings
//
// Complete settings screen matching all iOS SettingsView sections:
//   Enforcement · Economy · Emotional · AI · Social
//   Notifications · Data · About

package dev.spark.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.spark.app.BuildConfig

// MARK: - Enforcement mode

enum class EnforcementMode(val label: String) {
    SOFT_LOCK("Soft Lock"),
    HARD_LOCK("Hard Lock"),
    TRACK_ONLY("Track Only")
}

// MARK: - Sharing level

enum class SharingLevel(val label: String) {
    PRIVATE("Private"),
    FRIENDS("Friends"),
    CIRCLE("Circle"),
    PUBLIC("Public")
}

// MARK: - ViewModel

class SettingsViewModel : androidx.lifecycle.ViewModel() {
    // Enforcement
    var defaultMode by mutableStateOf(EnforcementMode.SOFT_LOCK)
    var cooldownMinutes by mutableFloatStateOf(15f)

    // Economy
    var fpEnabled by mutableStateOf(true)
    var dailyBaselineFP by mutableIntStateOf(60)
    var antiGamingEnabled by mutableStateOf(true)

    // Emotional
    var checkInEnabled by mutableStateOf(true)
    var coolingOffEnabled by mutableStateOf(true)

    // AI
    var cloudInsightsEnabled by mutableStateOf(true)
    var viewAnonymization by mutableStateOf(true)

    // Social
    var sharingLevel by mutableStateOf(SharingLevel.FRIENDS)

    // Notifications
    var nudgeNotifications by mutableStateOf(true)
    var insightNotifications by mutableStateOf(true)
    var challengeNotifications by mutableStateOf(true)
    var quietHoursEnabled by mutableStateOf(true)
    var quietStart by mutableStateOf("22:00")
    var quietEnd by mutableStateOf("08:00")

    // Dialogs
    var showDeleteDialog by mutableStateOf(false)
    var showDeleteAccountDialog by mutableStateOf(false)
    var showEnforcementPicker by mutableStateOf(false)
    var showSharingPicker by mutableStateOf(false)
    var isExporting by mutableStateOf(false)

    fun exportData() {
        isExporting = true
        // In production: serialize from KMP shared module and share via intent
        isExporting = false
    }

    fun deleteAllData() {
        // Calls KMP repository clear in production
    }

    fun deleteAccount() {
        deleteAllData()
        // Signs out from Supabase in production
    }
}

// MARK: - Root Screen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uriHandler = LocalUriHandler.current

    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog = false },
            title = { Text("Delete All Data") },
            text = { Text("This will permanently delete all your Bilbo data. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        viewModel.showDeleteDialog = false
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Your account and all associated data will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        viewModel.showDeleteAccountDialog = false
                    }
                ) { Text("Delete Account", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showEnforcementPicker) {
        EnforcementModePicker(
            selected = viewModel.defaultMode,
            onSelect = {
                viewModel.defaultMode = it
                viewModel.showEnforcementPicker = false
            },
            onDismiss = { viewModel.showEnforcementPicker = false }
        )
    }

    if (viewModel.showSharingPicker) {
        SharingLevelPicker(
            selected = viewModel.sharingLevel,
            onSelect = {
                viewModel.sharingLevel = it
                viewModel.showSharingPicker = false
            },
            onDismiss = { viewModel.showSharingPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        EnforcementSection(viewModel)
        EconomySection(viewModel)
        EmotionalSection(viewModel)
        AiSection(viewModel)
        SocialSection(viewModel)
        NotificationsSection(viewModel)
        DataSection(viewModel)
        AboutSection(onPrivacyClick = { uriHandler.openUri("https://getspark.app/privacy") })
    }
}

// MARK: - Section composables

@Composable
private fun EnforcementSection(vm: SettingsViewModel) {
    SettingsGroup(title = "Enforcement") {
        SettingsPickerRow(
            icon = Icons.Filled.Shield,
            label = "Default Mode",
            value = vm.defaultMode.label,
            onClick = { vm.showEnforcementPicker = true }
        )

        SettingsDivider()

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cooldown Duration", style = MaterialTheme.typography.bodyLarge)
                Text("${vm.cooldownMinutes.toInt()} min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = vm.cooldownMinutes,
                onValueChange = { vm.cooldownMinutes = it },
                valueRange = 5f..60f,
                steps = 10
            )
        }

        SettingsDivider()

        SettingsArrowRow(icon = Icons.Filled.Apps, label = "Per-App Overrides")
    }
}

@Composable
private fun EconomySection(vm: SettingsViewModel) {
    SettingsGroup(title = "Economy") {
        SettingsSwitchRow(
            icon = Icons.Filled.FlashOn,
            label = "Enable Focus Points",
            checked = vm.fpEnabled,
            onCheckedChange = { vm.fpEnabled = it }
        )

        if (vm.fpEnabled) {
            SettingsDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Baseline", style = MaterialTheme.typography.bodyLarge)
                    Text("FP awarded each day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (vm.dailyBaselineFP >= 10) vm.dailyBaselineFP -= 10 }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text("${vm.dailyBaselineFP} FP", modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { if (vm.dailyBaselineFP <= 190) vm.dailyBaselineFP += 10 }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }

            SettingsDivider()

            SettingsSwitchRow(
                icon = Icons.Filled.Security,
                label = "Anti-Gaming Protection",
                checked = vm.antiGamingEnabled,
                onCheckedChange = { vm.antiGamingEnabled = it }
            )
        }
    }
}

@Composable
private fun EmotionalSection(vm: SettingsViewModel) {
    SettingsGroup(title = "Emotional") {
        SettingsSwitchRow(
            icon = Icons.Filled.Favorite,
            label = "Emotional Check-Ins",
            checked = vm.checkInEnabled,
            onCheckedChange = { vm.checkInEnabled = it }
        )
        if (vm.checkInEnabled) {
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Filled.AcUnit,
                label = "Cooling-Off Mode",
                checked = vm.coolingOffEnabled,
                onCheckedChange = { vm.coolingOffEnabled = it }
            )
        }
    }
}

@Composable
private fun AiSection(vm: SettingsViewModel) {
    SettingsGroup(title = "AI Insights") {
        SettingsSwitchRow(
            icon = Icons.Filled.Psychology,
            label = "Cloud AI Insights",
            checked = vm.cloudInsightsEnabled,
            onCheckedChange = { vm.cloudInsightsEnabled = it }
        )
        if (vm.cloudInsightsEnabled) {
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Filled.VisibilityOff,
                label = "Anonymize Before Sending",
                checked = vm.viewAnonymization,
                onCheckedChange = { vm.viewAnonymization = it }
            )
            SettingsDivider()
            SettingsArrowRow(icon = Icons.Filled.Preview, label = "Preview Data Sent to AI")
        }
    }
}

@Composable
private fun SocialSection(vm: SettingsViewModel) {
    SettingsGroup(title = "Social") {
        SettingsPickerRow(
            icon = Icons.Filled.Lock,
            label = "Sharing Level",
            value = vm.sharingLevel.label,
            onClick = { vm.showSharingPicker = true }
        )
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.People, label = "Manage Accountability Buddies")
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.Groups, label = "Manage Circles")
    }
}

@Composable
private fun NotificationsSection(vm: SettingsViewModel) {
    SettingsGroup(title = "Notifications") {
        SettingsSwitchRow(Icons.Filled.Notifications, "Nudge Notifications", vm.nudgeNotifications) { vm.nudgeNotifications = it }
        SettingsDivider()
        SettingsSwitchRow(Icons.Filled.BarChart, "Weekly Insight Ready", vm.insightNotifications) { vm.insightNotifications = it }
        SettingsDivider()
        SettingsSwitchRow(Icons.Filled.EmojiEvents, "Challenge Updates", vm.challengeNotifications) { vm.challengeNotifications = it }
        SettingsDivider()
        SettingsSwitchRow(Icons.Filled.NightlightRound, "Quiet Hours", vm.quietHoursEnabled) { vm.quietHoursEnabled = it }
        if (vm.quietHoursEnabled) {
            SettingsDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.quietStart, style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("End", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(vm.quietEnd, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun DataSection(vm: SettingsViewModel) {
    SettingsGroup(title = "Data") {
        SettingsActionRow(
            icon = Icons.Filled.FileDownload,
            label = "Export All Data as JSON",
            onClick = { vm.exportData() },
            isLoading = vm.isExporting
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Filled.Delete,
            label = "Delete All Data",
            onClick = { vm.showDeleteDialog = true },
            isDestructive = true
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Filled.PersonRemove,
            label = "Delete Account",
            onClick = { vm.showDeleteAccountDialog = true },
            isDestructive = true
        )
    }
}

@Composable
private fun AboutSection(onPrivacyClick: () -> Unit) {
    SettingsGroup(title = "About") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Version", style = MaterialTheme.typography.bodyLarge)
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.Description, label = "Open-Source Licenses")
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.PrivacyTip, label = "Privacy Policy", onClick = onPrivacyClick)
    }
}

// MARK: - Reusable setting row components

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsPickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsArrowRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

// MARK: - Pickers

@Composable
fun EnforcementModePicker(
    selected: EnforcementMode,
    onSelect: (EnforcementMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Enforcement Mode") },
        text = {
            Column {
                EnforcementMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SharingLevelPicker(
    selected: SharingLevel,
    onSelect: (SharingLevel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sharing Level") },
        text = {
            Column {
                SharingLevel.entries.forEach { level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(level) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = level == selected, onClick = { onSelect(level) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(level.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}
