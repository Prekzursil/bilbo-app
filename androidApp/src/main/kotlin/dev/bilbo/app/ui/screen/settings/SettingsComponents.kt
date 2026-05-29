package dev.bilbo.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// MARK: - Reusable setting row components

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
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
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

private const val ROW_HORIZONTAL_PADDING_DP = 16
private const val ROW_VERTICAL_PADDING_TIGHT_DP = 4
private const val ROW_VERTICAL_PADDING_DP = 14
private const val ROW_ICON_SIZE_DP = 22
private const val ROW_TRAILING_ICON_SIZE_DP = 20
private const val ROW_SPACER_DP = 12
private const val ROW_TRAILING_SPACER_DP = 4
private const val PROGRESS_STROKE_DP = 2

@Composable
private fun SettingsRowScaffold(
    icon: ImageVector,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
    verticalPaddingDp: Int = ROW_VERTICAL_PADDING_DP,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val base =
        Modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable(enabled = enabled, onClick = onClick) else m }
            .padding(horizontal = ROW_HORIZONTAL_PADDING_DP.dp, vertical = verticalPaddingDp.dp)
    Row(modifier = base, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(ROW_ICON_SIZE_DP.dp))
        Spacer(modifier = Modifier.width(ROW_SPACER_DP.dp))
        content()
    }
}

@Composable
private fun ChevronTrailing() {
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(ROW_TRAILING_ICON_SIZE_DP.dp),
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRowScaffold(
        icon = icon,
        onClick = null,
        verticalPaddingDp = ROW_VERTICAL_PADDING_TIGHT_DP,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsPickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    SettingsRowScaffold(icon = icon, onClick = onClick) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(ROW_TRAILING_SPACER_DP.dp))
        ChevronTrailing()
    }
}

@Composable
fun SettingsArrowRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
) {
    SettingsRowScaffold(icon = icon, onClick = onClick) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        ChevronTrailing()
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    SettingsRowScaffold(icon = icon, onClick = onClick, enabled = !isLoading, iconTint = tint) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ROW_TRAILING_ICON_SIZE_DP.dp),
                strokeWidth = PROGRESS_STROKE_DP.dp,
            )
        }
    }
}
