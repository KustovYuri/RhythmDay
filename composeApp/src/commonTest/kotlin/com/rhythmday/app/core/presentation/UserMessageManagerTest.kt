package com.rhythmday.app.core.presentation

import com.rhythmday.app.data.remote.ApiException
import com.rhythmday.app.feature.auth.data.AuthenticationRequiredException
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class UserMessageManagerTest {
    @Test
    fun mapsKnownApiCodeToRussianMessage() {
        val error = ApiException(
            status = HttpStatusCode.Conflict,
            code = "ENTRY_VERSION_CONFLICT",
            fieldErrors = null,
            message = "Conflict"
        )

        assertEquals(
            "Запись изменилась на другом устройстве. Обновите данные",
            error.toRussianUserMessage()
        )
    }

    @Test
    fun mapsUnknownServerErrorWithoutExposingBackendText() {
        val error = ApiException(
            status = HttpStatusCode.InternalServerError,
            code = "UNEXPECTED_BACKEND_ERROR",
            fieldErrors = null,
            message = "Internal database details"
        )

        assertEquals(
            "Сервер временно недоступен. Попробуйте позже",
            error.toRussianUserMessage()
        )
    }

    @Test
    fun mapsAuthenticationAndLocalFallbacks() {
        assertEquals(
            "Войдите в аккаунт, чтобы обновить данные",
            AuthenticationRequiredException().toRussianUserMessage()
        )
        assertEquals(
            "Не удалось сохранить запись",
            IllegalStateException("Storage details").toRussianUserMessage(
                fallback = "Не удалось сохранить запись"
            )
        )
    }
}
