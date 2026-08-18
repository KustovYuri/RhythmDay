package com.rhythmday.app.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.rhythmday.app.data.remote.ApiException
import com.rhythmday.app.feature.auth.data.AuthenticationRequiredException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect

class UserMessageManager {
    private val mutableMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val messages = mutableMessages.asSharedFlow()

    fun show(message: String) {
        message.takeIf(String::isNotBlank)?.let(mutableMessages::tryEmit)
    }

    fun showError(error: Throwable, fallback: String = "Не удалось выполнить действие") {
        show(error.toRussianUserMessage(fallback))
    }
}

@Composable
fun UserMessageHost(manager: UserMessageManager) {
    val presenter = rememberUserMessagePresenter()
    val currentPresenter by rememberUpdatedState(presenter)

    LaunchedEffect(manager) {
        manager.messages.collect(currentPresenter)
    }
}

fun Throwable.toRussianUserMessage(fallback: String = "Не удалось выполнить действие"): String =
    when (this) {
        is ApiException -> apiErrorMessage()
        is AuthenticationRequiredException -> "Войдите в аккаунт, чтобы обновить данные"
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> "Сервер не ответил вовремя. Попробуйте ещё раз"
        is UnsupportedOperationException -> message ?: "Функция пока недоступна"
        else -> fallback
    }

private fun ApiException.apiErrorMessage(): String = when (code) {
    "EMAIL_ALREADY_REGISTERED" -> "Аккаунт с таким email уже существует"
    "INVALID_CREDENTIALS" -> "Неверный email или пароль"
    "ACCOUNT_SUSPENDED" -> "Аккаунт приостановлен"
    "INVALID_CURRENT_PASSWORD" -> "Неверный текущий пароль"
    "PASSWORD_NOT_CHANGED" -> "Новый пароль должен отличаться от текущего"
    "PASSWORD_CONFIRMATION_MISMATCH" -> "Пароли не совпадают"
    "PASSWORD_POLICY_VIOLATION" -> "Пароль не соответствует требованиям безопасности"
    "AUTH_RATE_LIMITED" -> "Слишком много попыток. Попробуйте позже"
    "INVALID_REFRESH_TOKEN",
    "REFRESH_TOKEN_REUSED" -> "Сессия истекла. Войдите снова"
    "INVALID_PASSWORD_RESET_TOKEN" -> "Ссылка для восстановления недействительна или устарела"
    "ENTRY_DATE_CONFLICT" -> "Запись за этот день уже существует"
    "ENTRY_VERSION_CONFLICT" -> "Запись изменилась на другом устройстве. Обновите данные"
    "ENTRY_NOT_FOUND" -> "Запись не найдена"
    "INVALID_DATE_RANGE" -> "Указан некорректный период"
    "TAG_ALREADY_EXISTS" -> "Такой тег уже существует"
    "TAG_NOT_FOUND" -> "Тег не найден"
    "TAG_NAME_INVALID" -> "Проверьте название тега"
    else -> status.defaultApiErrorMessage()
}

private fun HttpStatusCode.defaultApiErrorMessage(): String = when {
    this == HttpStatusCode.BadRequest || this == HttpStatusCode.UnprocessableEntity ->
        "Проверьте введённые данные"
    this == HttpStatusCode.Unauthorized -> "Сессия истекла. Войдите снова"
    this == HttpStatusCode.Forbidden -> "Недостаточно прав для этого действия"
    this == HttpStatusCode.NotFound -> "Запрошенные данные не найдены"
    this == HttpStatusCode.Conflict -> "Данные изменились. Обновите экран и попробуйте снова"
    this == HttpStatusCode.TooManyRequests -> "Слишком много запросов. Попробуйте позже"
    value >= 500 -> "Сервер временно недоступен. Попробуйте позже"
    else -> "Сервер отклонил запрос. Попробуйте ещё раз"
}

@Composable
internal expect fun rememberUserMessagePresenter(): (String) -> Unit
