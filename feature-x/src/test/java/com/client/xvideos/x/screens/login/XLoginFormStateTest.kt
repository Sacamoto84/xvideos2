package com.client.xvideos.x.screens.login

import com.client.xvideos.common.settings.element.SavedCredentials
import com.client.xvideos.x.feature.auth.XSignInResult
import com.client.xvideos.x.feature.auth.XSignInStep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class XLoginFormStateTest {

    private val nothingSaved = SavedCredentials(login = "", password = "", revision = 0)
    private val scope = TestScope()

    /** Вход, которым управляет тест: этап и итог отдаёт по команде. */
    private class FakeSignIn {
        val calls = mutableListOf<Pair<String, String>>()
        var onStep: ((XSignInStep) -> Unit)? = null
        var answer = CompletableDeferred<XSignInResult>()

        suspend fun signIn(login: String, password: String, step: (XSignInStep) -> Unit): XSignInResult {
            calls += login to password
            onStep = step
            return answer.await()
        }
    }

    private fun form(fake: FakeSignIn, saved: SavedCredentials = nothingSaved) =
        XLoginFormState(saved, scope, fake::signIn)

    @Test
    fun `удачный вход отмечает форму и снимает этап`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("  user@example.com ")
        form.updatePassword("secret")

        form.submit()
        scope.runCurrent()

        assertEquals(listOf("user@example.com" to "secret"), fake.calls)
        assertEquals(XSignInStep.OPENING, form.step)
        fake.onStep?.invoke(XSignInStep.CAPTCHA)
        assertEquals(XSignInStep.CAPTCHA, form.step)

        fake.answer.complete(XSignInResult.Success)
        scope.runCurrent()

        assertTrue(form.signedIn)
        assertNull(form.step)
        assertNull(form.error)
    }

    @Test
    fun `отказ сайта показывается ошибкой, форма остаётся открытой`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("user@example.com")
        form.updatePassword("wrong")

        form.submit()
        scope.runCurrent()
        fake.answer.complete(XSignInResult.Rejected("Неверный пароль"))
        scope.runCurrent()

        assertEquals("Неверный пароль", form.error)
        assertFalse(form.signedIn)
        assertNull(form.step)
    }

    @Test
    fun `без логина или пароля вход не начинается`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("user@example.com")

        form.submit()
        scope.runCurrent()

        assertTrue(fake.calls.isEmpty())
        assertEquals("Введите логин и пароль X", form.error)
        assertNull(form.step)
    }

    @Test
    fun `повторное нажатие во время входа второй вход не запускает`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("user@example.com")
        form.updatePassword("secret")

        form.submit()
        scope.runCurrent()
        form.submit()
        scope.runCurrent()

        assertEquals(1, fake.calls.size)
    }

    @Test
    fun `закрытие формы отменяет вход, его итог форму не трогает`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("user@example.com")
        form.updatePassword("secret")
        form.submit()
        scope.runCurrent()

        form.clear()
        fake.answer.complete(XSignInResult.Success)
        scope.runCurrent()

        assertFalse(form.signedIn)
        assertNull(form.step)
        assertEquals("", form.login)
        assertEquals("", form.password)
    }

    @Test
    fun `отменённый вход не сбрасывает этап следующего`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.updateLogin("user@example.com")
        form.updatePassword("secret")
        form.submit()
        // Первый вход ждёт ответа сайта, когда форму открывают заново.
        scope.runCurrent()

        form.prefill(nothingSaved)
        form.updateLogin("user@example.com")
        form.updatePassword("secret")
        fake.answer = CompletableDeferred()
        form.submit()
        scope.runCurrent()

        assertEquals(XSignInStep.OPENING, form.step)
    }

    @Test
    fun `форма подставляет сохранённый логин, но не пароль`() {
        val fake = FakeSignIn()
        val form = form(fake, SavedCredentials(login = "user@example.com", password = "secret", revision = 1))

        assertEquals("user@example.com", form.login)
        assertEquals("", form.password)
    }

    @Test
    fun `правка поля стирает прежнюю ошибку`() {
        val fake = FakeSignIn()
        val form = form(fake)
        form.submit()
        assertEquals("Введите логин и пароль X", form.error)

        form.updatePassword("s")

        assertNull(form.error)
    }
}
