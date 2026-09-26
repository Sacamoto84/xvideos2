package com.client.xvideos.l.model

import kotlinx.serialization.Serializable

/**
 * Модель учетных данных пользователя сервиса Luscious.
 *
 * Хранит связку логина (email) и пароля для прохождения аутентификации
 * и получения сессионных cookie/токенов в [com.client.xvideos.l.LSession].
 *
 * @property email Адрес электронной почты аккаунта.
 * @property password Пароль аккаунта.
 */
@Serializable
data class UserProfile(
    val email: String = "",
    val password: String = ""
) {
    /** Флаг валидности учетных данных: оба поля (email и пароль) заполнены. */
    val isValid: Boolean get() = email.isNotBlank() && password.isNotBlank()

    /** Флаг полной пустоты профиля (и email, и пароль пусты). */
    val isEmpty: Boolean get() = email.isEmpty() && password.isEmpty()

    /** Флаг наличия хотя бы одного заполненного поля. */
    val isNotEmpty: Boolean get() = !isEmpty

    /** Флаг наличия непустого email. */
    val hasEmail: Boolean get() = email.isNotBlank()

    /** Флаг наличия непустого пароля. */
    val hasPassword: Boolean get() = password.isNotBlank()

    /** Флаг готовности профиля к аутентификации (синоним [isValid]). */
    val isConfigured: Boolean get() = isValid

    /** Флаг наличия полной пары учетных данных (синоним [isValid]). */
    val hasCredentials: Boolean get() = isValid

    /** Маскированный пароль для безопасного отображения в логах. */
    val maskedPassword: String get() = if (password.isEmpty()) "" else "*".repeat(password.length.coerceAtMost(8))

    /** Проверяет совпадение пользователя по email без учета регистра. */
    fun isSameUser(other: UserProfile?): Boolean =
        other != null && email.isNotBlank() && email.equals(other.email, ignoreCase = true)


    companion object {
        /** Пустой профиль пользователя по умолчанию. */
        val EMPTY = UserProfile()
    }
}
