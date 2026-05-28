package dev.bilbo.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.bilbo.app.BuildConfig

// SettingsScreen.kt
// Bilbo — Android Settings
//
// Complete settings screen matching all iOS SettingsView sections:
//   Enforcement · Economy · Emotional · AI · Social
//   Notifications · Data · About

// MARK: - Enforcement mode

enum class EnforcementMode(
    val label: String,
) {
    SOFT_LOCK("Soft Lock"),
    HARD_LOCK("Hard Lock"),
    TRACK_ONLY("Track Only"),
}

// MARK: - Sharing level

enum class SharingLevel(
    val label: String,
) {
    PRIVATE("Private"),
    FRIENDS("Friends"),
    CIRCLE("Circle"),
    PUBLIC("Public"),
}

// MARK: - ViewModel

private const val DEFAULT_COOLDOWN_MINUTES = 15f
private const val DEFAULT_DAILY_BASELINE_FP = 60

class SettingsViewModel : androidx.lifecycle.ViewModel() {
    // Enforcement
    var defaultMode by mutableStateOf(EnforcementMode.SOFT_LOCK)
    var cooldownMinutes by mutableFloatStateOf(DEFAULT_COOLDOWN_MINUTES)

    // Economy
    var fpEnabled by mutableStateOf(true)
    var dailyBaselineFP by mutableIntStateOf(DEFAULT_DAILY_BASELINE_FP)
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
    viewModel: SettingsViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel(),
) {
    val uriHandler = LocalUriHandler.current

    SettingsDialogs(viewModel)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        EnforcementSection(viewModel)
        EconomySection(viewModel)
        EmotionalSection(viewModel)
        AiSection(viewModel)
        SocialSection(viewModel)
        NotificationsSection(viewModel)
        DataSection(viewModel)
        AboutSection(onPrivacyClick = { uriHandler.openUri("https://getbilbo.app/privacy") })
    }
}

@Composable
private fun SettingsDialogs(viewModel: SettingsViewModel) {
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
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog = false }) { Text("Cancel") }
            },
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
                    },
                ) { Text("Delete Account", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteAccountDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (viewModel.showEnforcementPicker) {
        EnforcementModePicker(
            selected = viewModel.defaultMode,
            onSelect = {
                viewModel.defaultMode = it
                viewModel.showEnforcementPicker = false
            },
            onDismiss = { viewModel.showEnforcementPicker = false },
        )
    }

    if (viewModel.showSharingPicker) {
        SharingLevelPicker(
            selected = viewModel.sharingLevel,
            onSelect = {
                viewModel.sharingLevel = it
                viewModel.showSharingPicker = false
            },
            onDismiss = { viewModel.showSharingPicker = false },
        )
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
            onClick = { vm.showEnforcementPicker = true },
        )

        SettingsDivider()

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cooldown Duration", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${vm.cooldownMinutes.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = vm.cooldownMinutes,
                onValueChange = { vm.cooldownMinutes = it },
                valueRange = 5f..60f,
                steps = 10,
            )
        }

        SettingsDivider()

        SettingsArrowRow(icon = Icons.Filled.Apps, label = "Per-App Overrides")
    }
}

private const val BASELINE_STEP_FP = 10
private const val BASELINE_MIN_FP = 10
private const val BASELINE_MAX_FP = 190
private const val BASELINE_VALUE_WIDTH_DP = 56

@Composable
private fun EconomySection(vm: SettingsViewModel) {
    SettingsGroup(title = "Economy") {
        SettingsSwitchRow(
            icon = Icons.Filled.FlashOn,
            label = "Enable Focus Points",
            checked = vm.fpEnabled,
            onCheckedChange = { vm.fpEnabled = it },
        )
        if (vm.fpEnabled) {
            EconomyDetailRows(vm)
        }
    }
}

@Composable
private fun EconomyDetailRows(vm: SettingsViewModel) {
    SettingsDivider()
    DailyBaselineRow(
        value = vm.dailyBaselineFP,
        onDecrease = { if (vm.dailyBaselineFP >= BASELINE_MIN_FP) vm.dailyBaselineFP -= BASELINE_STEP_FP },
        onIncrease = { if (vm.dailyBaselineFP <= BASELINE_MAX_FP) vm.dailyBaselineFP += BASELINE_STEP_FP },
    )
    SettingsDivider()
    SettingsSwitchRow(
        icon = Icons.Filled.Security,
        label = "Anti-Gaming Protection",
        checked = vm.antiGamingEnabled,
        onCheckedChange = { vm.antiGamingEnabled = it },
    )
}

@Composable
private fun DailyBaselineRow(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Daily Baseline", style = MaterialTheme.typography.bodyLarge)
            Text(
                "FP awarded each day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(
                "$value FP",
                modifier = Modifier.width(BASELINE_VALUE_WIDTH_DP.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onIncrease) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
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
            onCheckedChange = { vm.checkInEnabled = it },
        )
        if (vm.checkInEnabled) {
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Filled.AcUnit,
                label = "Cooling-Off Mode",
                checked = vm.coolingOffEnabled,
                onCheckedChange = { vm.coolingOffEnabled = it },
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
            onCheckedChange = { vm.cloudInsightsEnabled = it },
        )
        if (vm.cloudInsightsEnabled) {
            SettingsDivider()
            SettingsSwitchRow(
                icon = Icons.Filled.VisibilityOff,
                label = "Anonymize Before Sending",
                checked = vm.viewAnonymization,
                onCheckedChange = { vm.viewAnonymization = it },
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
            onClick = { vm.showSharingPicker = true },
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
        SettingsSwitchRow(Icons.Filled.Notifications, "Nudge Notifications", vm.nudgeNotifications) {
            vm.nudgeNotifications =
                it
        }
        SettingsDivider()
        SettingsSwitchRow(Icons.Filled.BarChart, "Weekly Insight Ready", vm.insightNotifications) {
            vm.insightNotifications =
                it
        }
        SettingsDivider()
        SettingsSwitchRow(Icons.Filled.EmojiEvents, "Challenge Updates", vm.challengeNotifications) {
            vm.challengeNotifications =
                it
        }
        SettingsDivider()
        SettingsSwitchRow(
            Icons.Filled.NightlightRound,
            "Quiet Hours",
            vm.quietHoursEnabled,
        ) { vm.quietHoursEnabled = it }
        if (vm.quietHoursEnabled) {
            SettingsDivider()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Start",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(vm.quietStart, style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "End",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            isLoading = vm.isExporting,
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Filled.Delete,
            label = "Delete All Data",
            onClick = { vm.showDeleteDialog = true },
            isDestructive = true,
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Filled.PersonRemove,
            label = "Delete Account",
            onClick = { vm.showDeleteAccountDialog = true },
            isDestructive = true,
        )
    }
}

@Composable
private fun AboutSection(onPrivacyClick: () -> Unit) {
    SettingsGroup(title = "About") {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Version", style = MaterialTheme.typography.bodyLarge)
            Text(
                BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.Description, label = "Open-Source Licenses")
        SettingsDivider()
        SettingsArrowRow(icon = Icons.Filled.PrivacyTip, label = "Privacy Policy", onClick = onPrivacyClick)
    }
}

// MARK: - Pickers

@Composable
fun EnforcementModePicker(
    selected: EnforcementMode,
    onSelect: (EnforcementMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Enforcement Mode") },
        text = {
            Column {
                EnforcementMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(mode) }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun SharingLevelPicker(
    selected: SharingLevel,
    onSelect: (SharingLevel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sharing Level") },
        text = {
            Column {
                SharingLevel.entries.forEach { level ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(level) }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = level == selected, onClick = { onSelect(level) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(level.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
