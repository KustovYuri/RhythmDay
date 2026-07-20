package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.runtime.Composable

class SpeechTextRequester(
    val isListening: Boolean,
    val toggle: () -> Unit
)

@Composable
expect fun rememberSpeechTextRequester(
    onTextRecognized: (String) -> Unit
): SpeechTextRequester
