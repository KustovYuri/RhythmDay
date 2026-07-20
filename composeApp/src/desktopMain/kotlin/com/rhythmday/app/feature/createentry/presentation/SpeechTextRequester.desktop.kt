package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSpeechTextRequester(
    onTextRecognized: (String) -> Unit
): SpeechTextRequester = remember {
    SpeechTextRequester(
        isListening = false,
        toggle = {}
    )
}
