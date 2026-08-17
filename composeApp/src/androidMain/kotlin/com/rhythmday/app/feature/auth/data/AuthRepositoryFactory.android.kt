package com.rhythmday.app.feature.auth.data

import android.content.Context
import com.rhythmday.app.feature.auth.domain.AuthRepository
import java.io.File

fun createAuthRepository(context: Context): AuthRepository {
    val file = File(context.applicationContext.filesDir, AUTH_PREFERENCES_FILE_NAME)
    return DataStoreAuthRepository(createAuthDataStore(file.absolutePath))
}
