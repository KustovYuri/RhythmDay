package com.rhythmday.app.feature.auth.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val email: String
) {
    val fullName: String
        get() = listOf(firstName, lastName).filter(String::isNotBlank).joinToString(" ")

    val initials: String
        get() = listOf(firstName, lastName)
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { value -> value.first().uppercase() }
            .ifBlank { email.firstOrNull()?.uppercase() ?: "Р" }
}

sealed interface AuthSession {
    data object Loading : AuthSession
    data object SignedOut : AuthSession
    data class Authenticated(val user: UserProfile) : AuthSession
}

interface AuthRepository {
    val session: StateFlow<AuthSession>

    suspend fun signIn(email: String)

    suspend fun register(firstName: String, lastName: String, email: String)

    suspend fun updateEmail(email: String)

    suspend fun signOut()
}

class InMemoryAuthRepository : AuthRepository {
    private val mutableSession = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
    private var savedProfile: UserProfile? = null

    override val session: StateFlow<AuthSession> = mutableSession

    override suspend fun signIn(email: String) {
        val normalizedEmail = email.trim().lowercase()
        val profile = savedProfile
            ?.takeIf { saved -> saved.email.equals(normalizedEmail, ignoreCase = true) }
            ?: UserProfile(
                firstName = normalizedEmail.substringBefore('@').asDisplayName(),
                lastName = "",
                email = normalizedEmail
            )
        savedProfile = profile
        mutableSession.value = AuthSession.Authenticated(profile)
    }

    override suspend fun register(firstName: String, lastName: String, email: String) {
        val profile = UserProfile(
            firstName = firstName.trim().asDisplayName(),
            lastName = lastName.trim().asDisplayName(),
            email = email.trim().lowercase()
        )
        savedProfile = profile
        mutableSession.value = AuthSession.Authenticated(profile)
    }

    override suspend fun updateEmail(email: String) {
        val profile = savedProfile ?: return
        val updated = profile.copy(email = email.trim().lowercase())
        savedProfile = updated
        mutableSession.value = AuthSession.Authenticated(updated)
    }

    override suspend fun signOut() {
        mutableSession.value = AuthSession.SignedOut
    }
}

internal fun String.asDisplayName(): String =
    lowercase().replaceFirstChar(Char::uppercase)
