package com.client.xvideos.x.feature.auth

import com.client.xvideos.x.urlStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XSignInScriptsTest {

    @Test
    fun `логин и пароль встраиваются литералами JSON`() {
        val script = XSignInScripts.fillFormAndStartCaptcha(
            login = "user@example.com",
            password = "a'b\"c\\d\n')</script>",
        )

        assertTrue(script.endsWith("""})("user@example.com", "a'b\"c\\d\n')</script>")"""))
        // Кавычка пароля не закрывает литерал: сырой перевод строки в скрипт не попал.
        assertFalse(script.contains("c\\d\n"))
    }

    @Test
    fun `скрипт заполнения отвечает известными состояниями`() {
        val script = XSignInScripts.fillFormAndStartCaptcha("user", "secret")

        listOf(
            XSignInScripts.FILL_STARTED,
            XSignInScripts.FILL_WAIT,
            XSignInScripts.FILL_NO_FORM,
            XSignInScripts.FILL_NO_CAPTCHA,
        ).forEach { status -> assertTrue(status, script.contains("'$status'")) }
    }

    @Test
    fun `состояние капчи по скрытому полю`() {
        assertEquals(CaptchaProgress.PENDING, captchaProgress(""))
        assertEquals(CaptchaProgress.PENDING, captchaProgress(".UNACTIVATED"))
        assertEquals(CaptchaProgress.PENDING, captchaProgress(".SOLVING"))
        assertEquals(CaptchaProgress.SOLVED, captchaProgress("AQQA.rBqKILLWAt_Vlex"))
        assertEquals(CaptchaProgress.FAILED, captchaProgress(".ERROR"))
        assertEquals(CaptchaProgress.FAILED, captchaProgress(".EXPIRED"))
        assertEquals(CaptchaProgress.FAILED, captchaProgress(".RESET"))
    }

    @Test
    fun `cookie касаются только сайта X и его поддоменов`() {
        val host = java.net.URI(urlStart).host
        val domain = host.removePrefix("www.")

        assertTrue(isXHost(host))
        assertTrue(isXHost(domain))
        assertTrue(isXHost("static.$domain"))
        assertFalse(isXHost("evil$domain"))
        assertFalse(isXHost("example.com"))
        assertFalse(isXHost(""))
    }
}
