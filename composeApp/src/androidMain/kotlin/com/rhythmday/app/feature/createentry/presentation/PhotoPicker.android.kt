package com.rhythmday.app.feature.createentry.presentation

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

@Composable
actual fun rememberPhotoPicker(
    onPhotosPicked: (List<ByteArray>) -> Unit,
    onError: () -> Unit
): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnPhotosPicked = rememberUpdatedState(onPhotosPicked)
    val latestOnError = rememberUpdatedState(onError)
    val hasCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var showCamera by remember { mutableStateOf(false) }

    val importCapturedPhoto: (File) -> Unit = { photoFile ->
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val photo = context.encodeCapturedPhoto(Uri.fromFile(photoFile))
                    context.saveCapturedPhotoToGallery(photoFile)
                    photo
                }
            }.onSuccess { photo -> latestOnPhotosPicked.value(listOf(photo)) }
                .onFailure { error ->
                    Log.e(CAMERA_LOG_TAG, "Unable to import captured photo", error)
                    latestOnError.value()
                }
            photoFile.delete()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (requiredCameraPermissions().all { result[it] == true }) {
            showCamera = true
        } else {
            latestOnError.value()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    uris.map { uri -> context.encodePhoto(uri) }
                }
            }.onSuccess { photos -> latestOnPhotosPicked.value(photos) }
                .onFailure { latestOnError.value() }
        }
    }

    if (showCamera) {
        CameraCaptureDialog(
            onDismiss = { showCamera = false },
            onPhotoCaptured = { photoFile ->
                showCamera = false
                importCapturedPhoto(photoFile)
            },
            onError = latestOnError.value
        )
    }

    return PhotoPicker(
        canSelectFromGallery = true,
        canTakePhoto = hasCamera,
        launchGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        launchCamera = {
            val permissions = requiredCameraPermissions()
            if (permissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) ==
                        PackageManager.PERMISSION_GRANTED
                }
            ) {
                showCamera = true
            } else {
                cameraPermissionLauncher.launch(permissions)
            }
        }
    )
}

@Composable
private fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (File) -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context, lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener(
            {
                if (disposed) return@addListener
                runCatching {
                    provider = providerFuture.get()
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                        )
                        .build()
                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(resolutionSelector)
                        .build()
                    provider?.unbindAll()
                    provider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    imageCapture = capture
                }.onFailure { error ->
                    Log.e(CAMERA_LOG_TAG, "Unable to start CameraX", error)
                    cameraError = "Не удалось открыть камеру"
                    onError()
                }
            },
            mainExecutor
        )
        onDispose {
            disposed = true
            provider?.unbindAll()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(CAMERA_PREVIEW_ASPECT_RATIO)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(14.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC152427))
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", color = Color.White, fontSize = 30.sp)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                cameraError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC152427))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(
                            enabled = imageCapture != null && !isCapturing,
                            role = Role.Button
                        ) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            cameraError = null
                            capture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                            val photoFile = context.createCameraTempFile()
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            capture.takePicture(
                                outputOptions,
                                mainExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        outputFileResults: ImageCapture.OutputFileResults
                                    ) {
                                        isCapturing = false
                                        onPhotoCaptured(photoFile)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        photoFile.delete()
                                        isCapturing = false
                                        cameraError = "Не удалось сделать фото"
                                        Log.e(CAMERA_LOG_TAG, "CameraX capture failed", exception)
                                        onError()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCapturing) 30.dp else 58.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

private fun requiredCameraPermissions(): Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}.toTypedArray()

private fun Context.createCameraTempFile(): File {
    val directory = File(cacheDir, CAMERA_CACHE_DIRECTORY).apply { mkdirs() }
    return File.createTempFile("day-photo-", ".jpg", directory)
}

@Suppress("DEPRECATION")
private fun Context.saveCapturedPhotoToGallery(photoFile: File) {
    val fileName = "RhythmDay_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/RhythmDay"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "RhythmDay"
            ).apply { mkdirs() }
            put(MediaStore.Images.Media.DATA, File(directory, fileName).absolutePath)
        }
    }
    val galleryUri = requireNotNull(
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    ) { "Unable to create a photo in the media gallery" }
    try {
        requireNotNull(contentResolver.openOutputStream(galleryUri)).use { output ->
            photoFile.inputStream().use { input -> input.copyTo(output) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishedValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            contentResolver.update(galleryUri, publishedValues, null, null)
        }
    } catch (error: Throwable) {
        contentResolver.delete(galleryUri, null, null)
        throw error
    }
}

private fun Context.encodePhoto(uri: Uri): ByteArray {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longestSide = maxOf(info.size.width, info.size.height)
            if (longestSide > MAX_PHOTO_SIDE) {
                val scale = MAX_PHOTO_SIDE.toFloat() / longestSide
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported photo format" }
        val sampleSize = generateSequence(1) { it * 2 }
            .first { maxOf(bounds.outWidth, bounds.outHeight) / it <= MAX_PHOTO_SIDE }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        requireNotNull(contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        })
    }

    return ByteArrayOutputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output))
        bitmap.recycle()
        output.toByteArray()
    }
}

private fun Context.encodeCapturedPhoto(uri: Uri): ByteArray {
    var lastFailure: Throwable? = null
    repeat(CAMERA_READ_ATTEMPTS) { attempt ->
        runCatching { encodePhoto(uri) }
            .onSuccess { return it }
            .onFailure { lastFailure = it }
        if (attempt < CAMERA_READ_ATTEMPTS - 1) Thread.sleep(CAMERA_READ_RETRY_DELAY_MILLIS)
    }
    throw lastFailure ?: IllegalStateException("Unable to read the captured photo")
}

private const val MAX_PHOTO_SIDE = 1600
private const val CAMERA_READ_ATTEMPTS = 4
private const val CAMERA_READ_RETRY_DELAY_MILLIS = 150L
private const val CAMERA_CACHE_DIRECTORY = "captured_photos"
private const val CAMERA_LOG_TAG = "RhythmDayCamera"
private const val CAMERA_PREVIEW_ASPECT_RATIO = 3f / 4f
