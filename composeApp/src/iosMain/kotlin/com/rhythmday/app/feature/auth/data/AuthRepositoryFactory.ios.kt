package com.rhythmday.app.feature.auth.data

import com.rhythmday.app.feature.auth.domain.AuthRepository
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createAuthRepository(): AuthRepository {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val filePath = requireNotNull(documentDirectory?.path) + "/$AUTH_PREFERENCES_FILE_NAME"
    return DataStoreAuthRepository(createAuthDataStore(filePath))
}
