package com.rhythmday.app.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberReminderPermissionRequester(
    onResult: (Boolean) -> Unit
): ReminderPermissionRequester {
    val currentOnResult = rememberUpdatedState(onResult)
    return remember {
        object : ReminderPermissionRequester {
            override fun request() {
                currentOnResult.value(true)
            }

            override fun check() {
                currentOnResult.value(true)
            }
        }
    }
}
