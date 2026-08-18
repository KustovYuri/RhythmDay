package com.rhythmday.app.feature.auth.data

import com.rhythmday.app.data.remote.BackendDependencies
import com.rhythmday.app.data.remote.createBackendDependencies
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createPlatformBackendDependencies(apiBaseUrl: String = ""): BackendDependencies {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val filePath = requireNotNull(documentDirectory?.path) + "/$AUTH_PREFERENCES_FILE_NAME"
    return createBackendDependencies(createAuthDataStore(filePath), apiBaseUrl)
}
