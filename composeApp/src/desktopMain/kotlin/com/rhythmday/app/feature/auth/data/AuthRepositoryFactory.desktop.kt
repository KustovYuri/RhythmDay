package com.rhythmday.app.feature.auth.data

import com.rhythmday.app.feature.auth.domain.AuthRepository
import java.io.File

fun createAuthRepository(): AuthRepository {
    val applicationDirectory = File(System.getProperty("user.home"), ".rhythmday")
    applicationDirectory.mkdirs()
    val file = File(applicationDirectory, AUTH_PREFERENCES_FILE_NAME)
    return DataStoreAuthRepository(createAuthDataStore(file.absolutePath))
}
