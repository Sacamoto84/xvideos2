package com.client.xvideos.x.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Разбор итога отправки формы входа. Ответы — те, что сайт дал 24.10.2026
 * тестовому аккаунту: верный пароль, неверный пароль, форма без решённой капчи.
 */
class XSignInResponseTest {

    @Test
    fun `принятая форма и открытая сессия — вход выполнен`() {
        val raw = """{"answered":true,"valid":true,"logged":true,"message":null,"fieldErrors":[],"formErrors":[]}"""

        assertEquals(XSignInResult.Success, parseSignInPayload(raw))
    }

    @Test
    fun `принятая форма без сессии — сбой, а не успех`() {
        val raw = """{"answered":true,"valid":true,"logged":false}"""

        assertEquals(
            XSignInResult.Failed("Сайт принял вход, но сессия не открылась"),
            parseSignInPayload(raw)
        )
    }

    @Test
    fun `неверный пароль — отказ с первой строкой текста сайта`() {
        val raw = """
            {"answered":true,"valid":false,"logged":false,"message":null,"fieldErrors":[],
             "formErrors":["Не удается войти. Пожалуйста, проверьте свой логин/пароль.\nЗабыли пароль ? Нажмите здесь."]}
        """.trimIndent()

        assertEquals(
            XSignInResult.Rejected("Не удается войти. Пожалуйста, проверьте свой логин/пароль."),
            parseSignInPayload(raw)
        )
    }

    @Test
    fun `ошибка капчи — сбой, логин и пароль сайт не проверял`() {
        val raw = """
            {"answered":true,"valid":false,"logged":false,
             "fieldErrors":[{"field":"form-group form-field-signin-form_fc-fc6adc7557070bb has-error",
                             "text":"Извините, но для завершения процедуры регистрации вам следует заполнить CAPTCHA,"}],
             "formErrors":[]}
        """.trimIndent()

        val result = parseSignInPayload(raw)

        assertEquals(XSignInResult.Failed("Сайт не принял проверку устройства. Попробуйте ещё раз"), result)
    }

    @Test
    fun `ошибка у поля пароля — отказ`() {
        val raw = """
            {"answered":true,"valid":false,
             "fieldErrors":[{"field":"form-group form-field-signin-form_password has-error","text":"Пароль слишком короткий"}]}
        """.trimIndent()

        assertEquals(XSignInResult.Rejected("Пароль слишком короткий"), parseSignInPayload(raw))
    }

    @Test
    fun `сообщение сайта без формы — отказ с этим сообщением`() {
        val raw = """{"answered":true,"valid":false,"message":"Аккаунт заблокирован"}"""

        assertEquals(XSignInResult.Rejected("Аккаунт заблокирован"), parseSignInPayload(raw))
    }

    @Test
    fun `сайт не ответил или ответ не разобрался — сбой`() {
        assertEquals(XSignInResult.Failed("Сайт не ответил на вход"), parseSignInPayload("""{"answered":false}"""))
        assertEquals(XSignInResult.Failed("Не удалось разобрать ответ сайта"), parseSignInPayload("не JSON"))
        assertEquals(XSignInResult.Failed("Сайт не принял вход"), parseSignInPayload("""{"answered":true}"""))
    }

    @Test
    fun `проверка входа читает is_logged`() {
        assertEquals(true, parseCheckLogin("""{"is_logged":true,"main_cat":"straight","result":true,"code":0}"""))
        assertEquals(false, parseCheckLogin("""{"is_logged":false,"result":true,"code":0}"""))
        assertNull(parseCheckLogin("""{"result":true}"""))
        assertNull(parseCheckLogin("<html>"))
    }
}
