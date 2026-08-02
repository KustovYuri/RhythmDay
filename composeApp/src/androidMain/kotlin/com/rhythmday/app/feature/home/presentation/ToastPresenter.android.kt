package com.rhythmday.app.feature.home.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberToastPresenter(): (String) -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
}
