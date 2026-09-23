package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberPhotoPicker(
    onPhotosPicked: (List<ByteArray>) -> Unit,
    onError: () -> Unit
): PhotoPicker {
    val scope = rememberCoroutineScope()
    val latestOnPhotosPicked = rememberUpdatedState(onPhotosPicked)
    val latestOnError = rememberUpdatedState(onError)
    return PhotoPicker(
        canSelectFromGallery = true,
        canTakePhoto = false,
        launchGallery = {
            val chooser = JFileChooser().apply {
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter("Фотографии", "jpg", "jpeg", "png", "webp")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val files: List<File> = chooser.selectedFiles.toList()
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { files.map(File::readBytes) } }
                        .onSuccess { latestOnPhotosPicked.value(it) }
                        .onFailure { latestOnError.value() }
                }
            }
        },
        launchCamera = {}
    )
}
