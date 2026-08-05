package com.rhythmday.app.feature.settings.presentation

import androidx.compose.runtime.Composable

interface ReminderPermissionRequester {
    fun request()

    fun check()
}

@Composable
expect fun rememberReminderPermissionRequester(
    onResult: (Boolean) -> Unit
): ReminderPermissionRequester
