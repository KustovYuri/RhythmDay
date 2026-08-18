package com.rhythmday.app.feature.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rhythmday.app.data.remote.ApiException
import com.rhythmday.app.data.remote.ChangePasswordRequestDto
import com.rhythmday.app.data.remote.LoginRequestDto
import com.rhythmday.app.data.remote.RegisterRequestDto
import com.rhythmday.app.data.remote.ResetPasswordRequestDto
import com.rhythmday.app.data.remote.RhythmDayApi
import com.rhythmday.app.data.remote.TokenResponseDto
import com.rhythmday.app.feature.auth.domain.AuthRepository
import com.rhythmday.app.feature.auth.domain.AuthSession
import com.rhythmday.app.feature.auth.domain.UserProfile
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path.Companion.toPath
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

const val AUTH_PREFERENCES_FILE_NAME = "auth.preferences_pb"

fun createAuthDataStore(filePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath {
        filePath.toPath()
    }

data class StoredCredentials(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val user: UserProfile
)

class AuthSessionStore(
    private val dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val session: StateFlow<AuthSession> = dataStore.data
        .map { preferences ->
            preferences.toStoredCredentials()?.let { credentials ->
                AuthSession.Authenticated(credentials.user)
            } ?: AuthSession.SignedOut
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthSession.Loading
        )

    suspend fun credentials(): StoredCredentials? =
        dataStore.data.first().toStoredCredentials()

    suspend fun save(response: TokenResponseDto) {
        dataStore.edit { preferences ->
            preferences[AccessTokenKey] = response.accessToken
            preferences[AccessTokenExpiresAtKey] = response.accessTokenExpiresAt
            preferences[RefreshTokenKey] = response.refreshToken
            preferences[RefreshTokenExpiresAtKey] = response.refreshTokenExpiresAt
            preferences[UserIdKey] = response.user.id
            preferences[FirstNameKey] = response.user.firstName
            preferences[LastNameKey] = response.user.lastName
            preferences[EmailKey] = response.user.email
            preferences[TimezoneKey] = response.user.timezone
        }
    }

    suspend fun updateEmail(email: String) {
        dataStore.edit { preferences -> preferences[EmailKey] = email.trim().lowercase() }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}

class AuthorizedSession(
    private val api: RhythmDayApi,
    private val store: AuthSessionStore,
    private val now: () -> Instant = Clock.System::now
) {
    private val refreshMutex = Mutex()

    suspend fun userId(): String? = store.credentials()?.user?.id

    suspend fun accessToken(): String = refreshMutex.withLock {
        val credentials = store.credentials() ?: throw AuthenticationRequiredException()
        if (credentials.accessTokenExpiresAt > now() + 30.seconds) {
            return@withLock credentials.accessToken
        }
        refresh(credentials)
    }

    suspend fun <T> request(block: suspend (accessToken: String) -> T): T {
        val token = accessToken()
        return try {
            block(token)
        } catch (error: ApiException) {
            if (error.status != HttpStatusCode.Unauthorized) throw error
            block(refreshAfterUnauthorized(token))
        }
    }

    private suspend fun refreshAfterUnauthorized(failedToken: String): String =
        refreshMutex.withLock {
            val credentials = store.credentials() ?: throw AuthenticationRequiredException()
            if (credentials.accessToken != failedToken) {
                credentials.accessToken
            } else {
                refresh(credentials)
            }
        }

    private suspend fun refresh(credentials: StoredCredentials): String {
        if (credentials.refreshTokenExpiresAt <= now()) {
            store.clear()
            throw AuthenticationRequiredException()
        }
        return try {
            api.refresh(credentials.refreshToken)
                .also { response -> store.save(response) }
                .accessToken
        } catch (error: ApiException) {
            if (error.status == HttpStatusCode.Unauthorized) store.clear()
            throw error
        }
    }

    suspend fun clear() = store.clear()
}

class RemoteAuthRepository(
    private val api: RhythmDayApi,
    private val store: AuthSessionStore,
    private val authorizedSession: AuthorizedSession
) : AuthRepository {
    override val session: StateFlow<AuthSession> = store.session

    override suspend fun signIn(email: String, password: String) {
        store.save(
            api.login(
                LoginRequestDto(
                    email = email.trim().lowercase(),
                    password = password
                )
            )
        )
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        timezone: String
    ) {
        store.save(
            api.register(
                RegisterRequestDto(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = email.trim().lowercase(),
                    password = password,
                    passwordConfirmation = passwordConfirmation,
                    timezone = timezone
                )
            )
        )
    }

    override suspend fun updateEmail(email: String) {
        throw UnsupportedOperationException("Смена email пока не поддерживается сервером")
    }

    override suspend fun requestPasswordReset(email: String) {
        api.forgotPassword(email.trim().lowercase())
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ) {
        authorizedSession.request { accessToken ->
            api.changePassword(
                accessToken = accessToken,
                request = ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    newPasswordConfirmation = newPasswordConfirmation
                )
            )
        }
        authorizedSession.clear()
    }

    override suspend fun resetPassword(
        token: String,
        newPassword: String,
        newPasswordConfirmation: String
    ) {
        api.resetPassword(
            ResetPasswordRequestDto(
                token = token,
                newPassword = newPassword,
                newPasswordConfirmation = newPasswordConfirmation
            )
        )
    }

    override suspend fun signOut() {
        val refreshToken = store.credentials()?.refreshToken
        authorizedSession.clear()
        if (refreshToken != null) runCatching { api.logout(refreshToken) }
    }
}

class AuthenticationRequiredException : IllegalStateException("Authentication is required")

private fun Preferences.toStoredCredentials(): StoredCredentials? {
    val accessToken = this[AccessTokenKey]?.takeIf(String::isNotBlank) ?: return null
    val refreshToken = this[RefreshTokenKey]?.takeIf(String::isNotBlank) ?: return null
    val accessExpiresAt = this[AccessTokenExpiresAtKey]?.toInstantOrNull() ?: return null
    val refreshExpiresAt = this[RefreshTokenExpiresAtKey]?.toInstantOrNull() ?: return null
    val email = this[EmailKey]?.takeIf(String::isNotBlank) ?: return null

    return StoredCredentials(
        accessToken = accessToken,
        accessTokenExpiresAt = accessExpiresAt,
        refreshToken = refreshToken,
        refreshTokenExpiresAt = refreshExpiresAt,
        user = UserProfile(
            id = this[UserIdKey],
            firstName = this[FirstNameKey].orEmpty().ifBlank { "Пользователь" },
            lastName = this[LastNameKey].orEmpty(),
            email = email,
            timezone = this[TimezoneKey] ?: "UTC"
        )
    )
}

private fun String.toInstantOrNull(): Instant? = runCatching(Instant::parse).getOrNull()

private val AccessTokenKey = stringPreferencesKey("access_token")
private val AccessTokenExpiresAtKey = stringPreferencesKey("access_token_expires_at")
private val RefreshTokenKey = stringPreferencesKey("refresh_token")
private val RefreshTokenExpiresAtKey = stringPreferencesKey("refresh_token_expires_at")
private val UserIdKey = stringPreferencesKey("user_id")
private val FirstNameKey = stringPreferencesKey("first_name")
private val LastNameKey = stringPreferencesKey("last_name")
private val EmailKey = stringPreferencesKey("email")
private val TimezoneKey = stringPreferencesKey("timezone")
