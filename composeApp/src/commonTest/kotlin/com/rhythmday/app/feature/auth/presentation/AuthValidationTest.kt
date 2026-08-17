package com.rhythmday.app.feature.auth.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthValidationTest {
    @Test
    fun acceptsValidFormValues() {
        assertNull(nameError("Мария"))
        assertNull(emailError("maria@example.com"))
        assertNull(passwordError("rhythm2026"))
        assertNull(confirmationError("rhythm2026", "rhythm2026"))
    }

    @Test
    fun rejectsInvalidEmailAndWeakPassword() {
        assertEquals("Проверьте формат email", emailError("maria@"))
        assertEquals("Минимум 8 символов", passwordError("day1"))
        assertEquals("Добавьте буквы и цифры", passwordError("rhythmday"))
        assertEquals("Пароли не совпадают", confirmationError("rhythm2026", "rhythm2025"))
    }
}
