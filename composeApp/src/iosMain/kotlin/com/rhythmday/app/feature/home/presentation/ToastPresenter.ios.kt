package com.rhythmday.app.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberToastPresenter(): (String) -> Unit = remember {
    { _: String -> }
}
