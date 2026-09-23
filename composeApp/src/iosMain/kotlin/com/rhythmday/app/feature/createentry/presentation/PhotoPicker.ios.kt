package com.rhythmday.app.feature.createentry.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPhotoPicker(
    onPhotosPicked: (List<ByteArray>) -> Unit,
    onError: () -> Unit
): PhotoPicker = remember {
    PhotoPicker(
        canSelectFromGallery = false,
        canTakePhoto = false,
        launchGallery = {},
        launchCamera = {}
    )
}
