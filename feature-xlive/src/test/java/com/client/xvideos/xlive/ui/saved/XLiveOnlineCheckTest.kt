package com.client.xvideos.xlive.ui.saved

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class XLiveOnlineCheckTest {

    private val asked = mutableListOf<String>()
    private val scope = CoroutineScope(Job() + Dispatchers.Unconfined)

    private fun check(answer: suspend (String) -> Boolean) = XLiveOnlineCheck(scope) { name ->
        asked += name
        answer(name)
    }

    @Test
    fun `ответы по всем комнатам, без повторов`() {
        val check = check { it == "a" }

        check.check(listOf("a", "b", "a"))

        assertEquals(listOf("a", "b"), asked)
        assertEquals(mapOf("a" to true, "b" to false), check.online.value)
        assertFalse(check.isChecking.value)
    }

    @Test
    fun `известные не спрашиваются заново, кроме свайпа`() {
        val check = check { true }
        check.check(listOf("a"))

        check.check(listOf("a", "b"))
        assertEquals(listOf("a", "b"), asked)

        check.check(listOf("a", "b"), force = true)
        assertEquals(listOf("a", "b", "a", "b"), asked)
    }

    @Test
    fun `комната без ответа остаётся непроверенной`() {
        val check = check { if (it == "a") throw IOException("нет сети") else true }

        check.check(listOf("a", "b"))

        assertEquals(mapOf("b" to true), check.online.value)
    }

    @Test
    fun `ответы публикуются разом, когда проверка закончилась`() {
        val gate = CompletableDeferred<Unit>()
        val check = check { name ->
            if (name == "b") gate.await()
            true
        }

        check.check(listOf("a", "b"))
        assertTrue(check.isChecking.value)
        assertEquals(emptyMap<String, Boolean>(), check.online.value)

        gate.complete(Unit)
        assertEquals(mapOf("a" to true, "b" to true), check.online.value)
        assertFalse(check.isChecking.value)
    }

    @Test
    fun `вызов во время проверки выполняется после неё`() {
        val gate = CompletableDeferred<Unit>()
        val check = check { name ->
            if (name == "a") gate.await()
            true
        }

        check.check(listOf("a"))
        check.check(listOf("c"))
        assertEquals(listOf("a"), asked)

        gate.complete(Unit)
        assertEquals(listOf("a", "c"), asked)
        assertEquals(mapOf("a" to true, "c" to true), check.online.value)
    }
}
