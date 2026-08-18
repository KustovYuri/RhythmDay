package com.rhythmday.app.feature.auth.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthValidationTest {
    @Test
    fun acceptsValidFormValues() {
        assertNull(nameError("Мария"))
        assertNull(emailError("maria@example.com"))
        assertNull(passwordError("rhythmday2026"))
        assertNull(confirmationError("rhythmday2026", "rhythmday2026"))
    }

    @Test
    fun rejectsInvalidEmailAndWeakPassword() {
        assertEquals("Проверьте формат email", emailError("maria@"))
        assertEquals("Минимум 12 символов", passwordError("day1"))
        assertEquals("Добавьте буквы и цифры", passwordError("rhythmdaylong"))
        assertEquals("Пароли не совпадают", confirmationError("rhythmday2026", "rhythmday2025"))
    }
}
