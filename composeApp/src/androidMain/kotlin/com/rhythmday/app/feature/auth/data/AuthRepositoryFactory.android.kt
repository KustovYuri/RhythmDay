package com.rhythmday.app.feature.auth.data

import android.content.Context
import com.rhythmday.app.data.remote.BackendDependencies
import com.rhythmday.app.data.remote.createBackendDependencies
import java.io.File

fun createPlatformBackendDependencies(
    context: Context,
    apiBaseUrl: String
): BackendDependencies {
    val file = File(context.applicationContext.filesDir, AUTH_PREFERENCES_FILE_NAME)
    return createBackendDependencies(createAuthDataStore(file.absolutePath), apiBaseUrl)
}
