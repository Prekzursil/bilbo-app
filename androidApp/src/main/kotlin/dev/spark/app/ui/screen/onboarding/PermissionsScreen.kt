// PermissionsScreen.kt
// Bilbo — Onboarding Screen 2: Permissions
//
// Lists required permissions with explanations, grant buttons,
// and green checkmarks when granted.

package dev.spark.app.ui.screen.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

// MARK: - Permission items

private data class PermissionItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val rationale: String,
    val isGranted: () -> Boolean,
    val onGrant: (Context) -> Unit
)

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Track re-composition triggers (e.g., returning from Settings)
    var refreshKey by remember { mutableIntStateOf(0) }

    val permissions = remember(refreshKey) {
        buildPermissionItems(context)
    }

    val allGranted = permissions.all { it.isGranted() }

    // Notification permission launcher (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshKey++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp)
    ) {
        // Back
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Permissions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "Bilbo needs these to protect your focus.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            permissions.forEach { item ->
                PermissionCard(
                    item = item,
                    onGrant = {
                        if (item.id == "notifications" && Build.VERSION.SDK_INT >= 33) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            item.onGrant(context)
                        }
                        refreshKey++
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = allGranted
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (!allGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onContinue,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    onGrant: () -> Unit
) {
    val granted = item.isGranted()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = item.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (granted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onGrant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

// MARK: - Permission definitions

private fun buildPermissionItems(context: Context): List<PermissionItem> = listOf(
    PermissionItem(
        id = "usage_access",
        icon = Icons.Filled.Visibility,
        title = "Usage Access",
        rationale = "So Bilbo can see which apps are running",
        isGranted = { hasUsageAccess(context) },
        onGrant = { ctx ->
            ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    ),
    PermissionItem(
        id = "overlay",
        icon = Icons.Filled.Layers,
        title = "Display Over Other Apps",
        rationale = "So Bilbo can show you the Intent Gatekeeper",
        isGranted = { Settings.canDrawOverlays(context) },
        onGrant = { ctx ->
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    ),
    PermissionItem(
        id = "notifications",
        icon = Icons.Filled.Notifications,
        title = "Notifications",
        rationale = "So Bilbo can remind you when time is up",
        isGranted = {
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true // Pre-13 always granted
            }
        },
        onGrant = { ctx ->
            if (Build.VERSION.SDK_INT < 33) {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            // For Android 13+ the caller uses ActivityResultLauncher
        }
    )
)

private fun hasUsageAccess(context: Context): Boolean {
    return try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionsScreenPreview() {
    PermissionsScreen(onContinue = {}, onBack = {})
}
