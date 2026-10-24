package com.client.xvideos.x.feature.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val signInJson = Json { ignoreUnknownKeys = true }

/** Класс группы поля капчи в форме входа: `form-field-signin-form_fc-<id>`. */
private const val CAPTCHA_FIELD_MARK = "signin-form_fc-"

/** Итог отправки формы, который собирает [XSignInScripts.SUBMIT]. */
@Serializable
internal data class SignInPayload(
    /** Сайт ответил на отправку формы JSON-ом. */
    val answered: Boolean = false,
    /** `form_valid` ответа: сайт принял форму. */
    val valid: Boolean = false,
    /** `is_logged` проверки входа после принятой формы. */
    val logged: Boolean = false,
    /** `message` ответа, если сайт его прислал. */
    val message: String? = null,
    /** Ошибки у полей формы. */
    val fieldErrors: List<SignInFieldError> = emptyList(),
    /** Ошибки формы целиком: неверная пара логин/пароль. */
    val formErrors: List<String> = emptyList(),
)

/**
 * Ошибка у поля формы входа.
 *
 * @property field классы группы поля; по ним видно, какое это поле.
 * @property text текст ошибки.
 */
@Serializable
internal data class SignInFieldError(
    val field: String = "",
    val text: String = "",
)

/**
 * Итог входа по строке, которую оставил [XSignInScripts.SUBMIT].
 *
 * Ошибка капчи — [XSignInResult.Failed], а не отказ: логин и пароль сайт в
 * этом случае не проверял, повтор может пройти. Отказом считается только то,
 * что сайт сказал про сами логин и пароль.
 */
internal fun parseSignInPayload(raw: String): XSignInResult {
    val payload = runCatching { signInJson.decodeFromString(SignInPayload.serializer(), raw) }
        .getOrNull()
        ?: return XSignInResult.Failed("Не удалось разобрать ответ сайта")

    if (!payload.answered) return XSignInResult.Failed("Сайт не ответил на вход")
    if (payload.valid) {
        return if (payload.logged) {
            XSignInResult.Success
        } else {
            XSignInResult.Failed("Сайт принял вход, но сессия не открылась")
        }
    }

    val (captchaErrors, credentialErrors) = payload.fieldErrors.partition { CAPTCHA_FIELD_MARK in it.field }
    val rejection = payload.formErrors.firstOrNull()
        ?: credentialErrors.firstOrNull()?.text
        ?: payload.message?.takeIf { it.isNotBlank() }

    return when {
        rejection != null -> XSignInResult.Rejected(firstLine(rejection))
        captchaErrors.isNotEmpty() -> XSignInResult.Failed("Сайт не принял проверку устройства. Попробуйте ещё раз")
        else -> XSignInResult.Failed("Сайт не принял вход")
    }
}

/**
 * `is_logged` ответа `POST /account/checklogin`; `null` — ответ не разобрался.
 */
internal fun parseCheckLogin(body: String): Boolean? = runCatching {
    signInJson.parseToJsonElement(body).jsonObject["is_logged"]?.jsonPrimitive?.booleanOrNull
}.getOrNull()

/** Ошибка сайта бывает с хвостом вроде «Забыли пароль? Нажмите здесь» — форме нужна первая строка. */
private fun firstLine(text: String): String =
    text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: text.trim()
