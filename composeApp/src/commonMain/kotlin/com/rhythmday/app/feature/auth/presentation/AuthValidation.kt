package com.rhythmday.app.feature.auth.presentation

internal fun nameError(value: String): String? = when {
    value.isBlank() -> "Заполните поле"
    value.trim().length < 2 -> "Минимум 2 символа"
    else -> null
}

internal fun emailError(value: String): String? = when {
    value.isBlank() -> "Введите email"
    !EmailPattern.matches(value.trim()) -> "Проверьте формат email"
    else -> null
}

internal fun passwordError(value: String): String? = when {
    value.isBlank() -> "Введите пароль"
    value.length < 8 -> "Минимум 8 символов"
    value.none(Char::isLetter) || value.none(Char::isDigit) -> "Добавьте буквы и цифры"
    else -> null
}

internal fun confirmationError(password: String, confirmation: String): String? = when {
    confirmation.isBlank() -> "Повторите пароль"
    password != confirmation -> "Пароли не совпадают"
    else -> null
}

private val EmailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
