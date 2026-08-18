package com.rhythmday.app.feature.auth.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InMemoryAuthRepositoryTest {
    @Test
    fun registrationUpdatesProfileAndSignOutKeepsItForNextLogin() = runTest {
        val repository = InMemoryAuthRepository()

        repository.register(
            firstName = "  иВАН ",
            lastName = " иВАНОВ ",
            email = " USER@Example.COM ",
            password = "rhythmday2026",
            passwordConfirmation = "rhythmday2026",
            timezone = "Europe/Moscow"
        )

        val registered = assertIs<AuthSession.Authenticated>(repository.session.value)
        assertEquals("Иван", registered.user.firstName)
        assertEquals("Иванов", registered.user.lastName)
        assertEquals("user@example.com", registered.user.email)

        repository.signOut()
        assertEquals(AuthSession.SignedOut, repository.session.value)

        repository.signIn("user@example.com", "rhythmday2026")
        val signedIn = assertIs<AuthSession.Authenticated>(repository.session.value)
        assertEquals("Иван Иванов", signedIn.user.fullName)
    }

    @Test
    fun emailUpdateIsVisibleInCurrentSession() = runTest {
        val repository = InMemoryAuthRepository()
        repository.register(
            firstName = "Анна",
            lastName = "Смирнова",
            email = "old@example.com",
            password = "rhythmday2026",
            passwordConfirmation = "rhythmday2026",
            timezone = "Europe/Moscow"
        )

        repository.updateEmail(" NEW@Example.COM ")

        val session = assertIs<AuthSession.Authenticated>(repository.session.value)
        assertEquals("new@example.com", session.user.email)
    }
}
