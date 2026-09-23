package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.runtime.Composable

class PhotoPicker(
    val canSelectFromGallery: Boolean,
    val canTakePhoto: Boolean,
    val launchGallery: () -> Unit,
    val launchCamera: () -> Unit
) {
    val isAvailable: Boolean
        get() = canSelectFromGallery || canTakePhoto
}

@Composable
expect fun rememberPhotoPicker(
    onPhotosPicked: (List<ByteArray>) -> Unit,
    onError: () -> Unit
): PhotoPicker
