package com.rhythmday.app.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rhythmday.app.feature.auth.data.AuthSessionStore
import com.rhythmday.app.feature.auth.data.AuthorizedSession
import com.rhythmday.app.feature.auth.data.RemoteAuthRepository
import com.rhythmday.app.feature.auth.domain.AuthRepository

data class BackendDependencies(
    val api: RhythmDayApi,
    val authorizedSession: AuthorizedSession,
    val authRepository: AuthRepository
)

fun createBackendDependencies(
    authDataStore: DataStore<Preferences>,
    apiBaseUrl: String
): BackendDependencies {
    val api = RhythmDayApi(createRhythmDayHttpClient(), apiBaseUrl)
    val store = AuthSessionStore(authDataStore)
    val authorizedSession = AuthorizedSession(api, store)
    return BackendDependencies(
        api = api,
        authorizedSession = authorizedSession,
        authRepository = RemoteAuthRepository(api, store, authorizedSession)
    )
}
