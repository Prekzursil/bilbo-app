package dev.spark.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

/**
 * Restarts the [UsageMonitorService] after the device reboots.
 *
 * Requires the [android.Manifest.permission.RECEIVE_BOOT_COMPLETED] permission,
 * declared in AndroidManifest.xml.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("Device rebooted — restarting UsageMonitorService")

        val serviceIntent = Intent(context, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
