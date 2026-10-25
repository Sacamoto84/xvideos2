package com.client.xvideos.xlive.ui.login

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Без runTest: неограниченный диспетчер гонит корутины screenModelScope до
// конца прямо внутри вызова, пока их не держит незавершённый Deferred.
@OptIn(ExperimentalCoroutinesApi::class)
class XLiveLoginSMTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `форма открывается, когда прежний выход закончился`() {
        val signOut = CompletableDeferred<Unit>()
        val sm = XLiveLoginSM(awaitSignOut = { signOut.await() }, confirmSignIn = { true })
        assertFalse(sm.ready.value)

        signOut.complete(Unit)

        assertTrue(sm.ready.value)
    }

    @Test
    fun `сайт подтвердил вход — экран закрывается`() {
        val sm = XLiveLoginSM(awaitSignOut = {}, confirmSignIn = { true })

        sm.onLeftSignInPages()

        assertTrue(sm.done.value)
    }

    @Test
    fun `не подтвердил — экран остаётся, следующая страница проверяется снова`() {
        val answers = mutableListOf(false, true)
        var asked = 0
        val sm = XLiveLoginSM(awaitSignOut = {}, confirmSignIn = { asked++; answers.removeAt(0) })

        sm.onLeftSignInPages()
        assertFalse(sm.done.value)

        sm.onLeftSignInPages()
        assertTrue(sm.done.value)
        assertEquals(2, asked)
    }

    @Test
    fun `пока идёт проверка, новая не начинается`() {
        val answer = CompletableDeferred<Boolean>()
        var asked = 0
        val sm = XLiveLoginSM(awaitSignOut = {}, confirmSignIn = { asked++; answer.await() })

        sm.onLeftSignInPages()
        sm.onLeftSignInPages()
        answer.complete(true)

        assertEquals(1, asked)
        assertTrue(sm.done.value)
    }
}
