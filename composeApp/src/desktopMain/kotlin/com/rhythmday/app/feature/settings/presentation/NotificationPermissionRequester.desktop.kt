package com.rhythmday.app.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester {
    val currentOnResult = rememberUpdatedState(onResult)
    return remember {
        object : NotificationPermissionRequester {
            override fun request() {
                currentOnResult.value(true)
            }
        }
    }
}
