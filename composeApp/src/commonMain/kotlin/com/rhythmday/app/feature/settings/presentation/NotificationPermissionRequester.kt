package com.rhythmday.app.feature.settings.presentation

import androidx.compose.runtime.Composable

interface NotificationPermissionRequester {
    fun request()
}

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): NotificationPermissionRequester
