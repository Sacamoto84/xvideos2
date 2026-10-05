package com.client.xvideos.l.ui.screens

import com.client.xvideos.common.settings.element.SavedCredentials
import org.junit.Assert.assertEquals
import org.junit.Test

class LLoginFormStateTest {

    private val nothingSaved = SavedCredentials(login = "", password = "", revision = 0)

    @Test
    fun `набранные логин и пароль остаются в состоянии формы`() {
        val form = LLoginFormState(nothingSaved)

        form.updateLogin("user")
        form.updatePassword("secret")

        assertEquals("user", form.login)
        assertEquals("secret", form.password)
    }

    @Test
    fun `незавершённый профиль подставляется в поля`() {
        val form = LLoginFormState(SavedCredentials(login = "user", password = "", revision = 3))

        assertEquals("user", form.login)
        assertEquals("", form.password)
    }

    @Test
    fun `завершённый профиль в форму не копируется`() {
        val form = LLoginFormState(SavedCredentials(login = "user", password = "secret", revision = 3))

        assertEquals("", form.login)
        assertEquals("", form.password)
    }

    @Test
    fun `повторное открытие формы заменяет набранное сохранённым`() {
        val form = LLoginFormState(nothingSaved)
        form.updateLogin("typed")
        form.updatePassword("typed-secret")

        form.prefill(SavedCredentials(login = "user", password = "", revision = 1))

        assertEquals("user", form.login)
        assertEquals("", form.password)
    }

    @Test
    fun `закрытие формы стирает набранное`() {
        val form = LLoginFormState(nothingSaved)
        form.updateLogin("user")
        form.updatePassword("secret")

        form.clear()

        assertEquals("", form.login)
        assertEquals("", form.password)
    }
}
