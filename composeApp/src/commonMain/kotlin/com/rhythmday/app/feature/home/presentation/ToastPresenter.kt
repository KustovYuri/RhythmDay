package com.rhythmday.app.feature.home.presentation

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberToastPresenter(): (String) -> Unit
