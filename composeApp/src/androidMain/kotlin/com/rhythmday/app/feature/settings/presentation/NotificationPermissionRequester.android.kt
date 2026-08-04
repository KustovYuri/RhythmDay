package com.rhythmday.app.feature.settings.presentation

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> currentOnResult.value(granted) }
    )

    return remember(context, launcher) {
        object : NotificationPermissionRequester {
            override fun request() {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED -> {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    context.notificationsEnabled() -> currentOnResult.value(true)
                    else -> currentOnResult.value(false)
                }
            }
        }
    }
}

private fun Context.notificationsEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
    return getSystemService(NotificationManager::class.java).areNotificationsEnabled()
}
