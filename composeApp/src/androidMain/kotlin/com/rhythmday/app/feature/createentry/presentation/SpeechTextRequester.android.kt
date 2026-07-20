package com.rhythmday.app.feature.createentry.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberSpeechTextRequester(
    onTextRecognized: (String) -> Unit
): SpeechTextRequester {
    val context = LocalContext.current
    val latestOnTextRecognized = rememberUpdatedState(onTextRecognized)
    var isListening by remember { mutableStateOf(false) }
    val controller = remember(context.applicationContext) {
        AndroidSpeechRecognitionController(
            context = context.applicationContext,
            onTextRecognized = { text -> latestOnTextRecognized.value(text) },
            onListeningChanged = { listening -> isListening = listening }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) controller.start()
    }

    DisposableEffect(controller) {
        onDispose(controller::destroy)
    }

    return SpeechTextRequester(
        isListening = isListening,
        toggle = {
            if (isListening) {
                controller.stop()
            } else if (
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                controller.start()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    )
}

private class AndroidSpeechRecognitionController(
    context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit
) : RecognitionListener {
    private val handler = Handler(Looper.getMainLooper())
    private val recognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@AndroidSpeechRecognitionController)
        }
    } else {
        null
    }
    private val recognitionIntent = createRecognitionIntent()
    private val restart = Runnable(::startSession)
    private var listeningRequested = false

    fun start() {
        if (listeningRequested || recognizer == null) return

        listeningRequested = true
        onListeningChanged(true)
        startSession()
    }

    fun stop() {
        if (!listeningRequested) return

        listeningRequested = false
        handler.removeCallbacks(restart)
        runCatching { recognizer?.stopListening() }
        onListeningChanged(false)
    }

    fun destroy() {
        listeningRequested = false
        handler.removeCallbacks(restart)
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        onListeningChanged(false)
    }

    override fun onResults(results: Bundle) {
        results.bestRecognizedText()?.let(onTextRecognized)
        scheduleRestart()
    }

    override fun onError(error: Int) {
        if (!listeningRequested) return

        if (error.isRecoverableSpeechError()) {
            scheduleRestart()
        } else {
            listeningRequested = false
            onListeningChanged(false)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun startSession() {
        if (!listeningRequested) return

        runCatching { recognizer?.startListening(recognitionIntent) }
            .onFailure { scheduleRestart() }
    }

    private fun scheduleRestart() {
        if (!listeningRequested) return

        handler.removeCallbacks(restart)
        handler.postDelayed(restart, RESTART_DELAY_MILLIS)
    }
}

private fun createRecognitionIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, PAUSE_MILLIS)
        putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
            PAUSE_MILLIS
        )
    }

private fun Bundle.bestRecognizedText(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf(String::isNotEmpty)

private fun Int.isRecoverableSpeechError(): Boolean =
    this == SpeechRecognizer.ERROR_NO_MATCH ||
        this == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
        this == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
        this == SpeechRecognizer.ERROR_CLIENT

private const val RESTART_DELAY_MILLIS = 250L
private const val PAUSE_MILLIS = 60_000L
