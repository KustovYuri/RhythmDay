package com.rhythmday.app.feature.settings.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberReminderPermissionRequester(
    onResult: (Boolean) -> Unit
): ReminderPermissionRequester {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    lateinit var requestExactAlarmPermission: () -> Unit
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { currentOnResult.value(context.hasReminderPermissions()) }
    )
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (context.notificationsEnabled()) {
                requestExactAlarmPermission()
            } else {
                currentOnResult.value(false)
            }
        }
    )
    requestExactAlarmPermission = {
        if (context.canScheduleExactReminders()) {
            currentOnResult.value(true)
        } else {
            exactAlarmLauncher.launch(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) requestExactAlarmPermission() else currentOnResult.value(false)
        }
    )

    return remember(
        context,
        exactAlarmLauncher,
        notificationPermissionLauncher,
        notificationSettingsLauncher
    ) {
        object : ReminderPermissionRequester {
            override fun request() {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED -> {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    !context.notificationsEnabled() -> {
                        notificationSettingsLauncher.launch(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }

                    else -> requestExactAlarmPermission()
                }
            }

            override fun check() {
                currentOnResult.value(context.hasReminderPermissions())
            }
        }
    }
}

private fun Context.hasReminderPermissions(): Boolean =
    notificationsEnabled() && canScheduleExactReminders()

private fun Context.canScheduleExactReminders(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

private fun Context.notificationsEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
    return getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}
