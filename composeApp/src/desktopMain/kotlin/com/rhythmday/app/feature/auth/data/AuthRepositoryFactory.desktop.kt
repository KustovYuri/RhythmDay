package com.rhythmday.app.feature.auth.data

import com.rhythmday.app.data.remote.BackendDependencies
import com.rhythmday.app.data.remote.createBackendDependencies
import java.io.File

fun createPlatformBackendDependencies(apiBaseUrl: String = ""): BackendDependencies {
    val applicationDirectory = File(System.getProperty("user.home"), ".rhythmday")
    applicationDirectory.mkdirs()
    val file = File(applicationDirectory, AUTH_PREFERENCES_FILE_NAME)
    return createBackendDependencies(createAuthDataStore(file.absolutePath), apiBaseUrl)
}
