package com.rhythmday.app.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberUserMessagePresenter(): (String) -> Unit = remember {
    { _: String -> }
}
