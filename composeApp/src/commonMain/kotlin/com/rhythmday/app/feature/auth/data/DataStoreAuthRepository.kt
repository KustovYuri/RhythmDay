package com.rhythmday.app.feature.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.rhythmday.app.feature.auth.domain.AuthRepository
import com.rhythmday.app.feature.auth.domain.AuthSession
import com.rhythmday.app.feature.auth.domain.UserProfile
import com.rhythmday.app.feature.auth.domain.asDisplayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okio.Path.Companion.toPath

const val AUTH_PREFERENCES_FILE_NAME = "auth.preferences_pb"

fun createAuthDataStore(filePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath {
        filePath.toPath()
    }

class DataStoreAuthRepository(
    private val dataStore: DataStore<Preferences>
) : AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val session: StateFlow<AuthSession> = dataStore.data
        .map { preferences -> preferences.toAuthSession() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthSession.Loading
        )

    override suspend fun signIn(email: String) {
        val normalizedEmail = email.trim().lowercase()
        dataStore.edit { preferences ->
            val savedEmail = preferences[EmailKey]
            if (!savedEmail.equals(normalizedEmail, ignoreCase = true)) {
                preferences[FirstNameKey] = normalizedEmail.substringBefore('@').asDisplayName()
                preferences[LastNameKey] = ""
            }
            preferences[EmailKey] = normalizedEmail
            preferences[AuthenticatedKey] = true
        }
    }

    override suspend fun register(firstName: String, lastName: String, email: String) {
        dataStore.edit { preferences ->
            preferences[FirstNameKey] = firstName.trim().asDisplayName()
            preferences[LastNameKey] = lastName.trim().asDisplayName()
            preferences[EmailKey] = email.trim().lowercase()
            preferences[AuthenticatedKey] = true
        }
    }

    override suspend fun updateEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[EmailKey] = email.trim().lowercase()
        }
    }

    override suspend fun signOut() {
        dataStore.edit { preferences ->
            preferences[AuthenticatedKey] = false
        }
    }
}

private fun Preferences.toAuthSession(): AuthSession {
    if (this[AuthenticatedKey] != true) {
        return AuthSession.SignedOut
    }

    val email = this[EmailKey].orEmpty()
    if (email.isBlank()) return AuthSession.SignedOut

    return AuthSession.Authenticated(
        UserProfile(
            firstName = this[FirstNameKey].orEmpty().ifBlank { "Пользователь" },
            lastName = this[LastNameKey].orEmpty(),
            email = email
        )
    )
}

private val AuthenticatedKey = booleanPreferencesKey("authenticated")
private val FirstNameKey = stringPreferencesKey("first_name")
private val LastNameKey = stringPreferencesKey("last_name")
private val EmailKey = stringPreferencesKey("email")
