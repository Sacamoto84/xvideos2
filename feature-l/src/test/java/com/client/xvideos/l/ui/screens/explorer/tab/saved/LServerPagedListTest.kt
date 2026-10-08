package com.client.xvideos.l.ui.screens.explorer.tab.saved

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Постраничный список вкладок «лайки на сервере» и «подписки»: сбой подгрузки
 * не должен запускать её по кругу, а обновление и подгрузка — идти одновременно.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LServerPagedListTest {

    /** Сервер списка: страницы отвечают так, как велит тест; запросы записываются. */
    private class Pages {
        val requests = mutableListOf<Int>()
        var respond: suspend (page: Int) -> Result<List<String>> = { page -> Result.success(listOf("item$page")) }

        suspend fun load(page: Int): Result<List<String>> {
            requests += page
            return respond(page)
        }
    }

    private val pages = Pages()
    private val failureNotices = mutableListOf<String>()
    private val replacements = mutableListOf<List<String>>()

    private fun TestScope.pagedList() = LServerPagedList(
        // Не backgroundScope: его задачи advanceUntilIdle() не выполняет.
        scope = this,
        loadPage = pages::load,
        notifyNextPageFailed = { failureNotices += it },
        onReplaced = { replacements += it },
    )

    @Test
    fun `сбой подгрузки не запускает её по кругу`() = runTest {
        pages.respond = { page -> if (page == 2) Result.failure(IOException("обрыв")) else Result.success(listOf("item$page")) }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        list.loadNextPage()
        advanceUntilIdle()
        // Экран зовёт подгрузку снова, как только признак загрузки погас.
        list.loadNextPage()
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), pages.requests)
        assertEquals(1, failureNotices.size)
        assertTrue(list.nextPageFailed.value)
        assertFalse(list.isLoading.value)
    }

    @Test
    fun `после ухода от конца списка подгрузку пробуют ещё раз`() = runTest {
        var secondPageFails = true
        pages.respond = { page ->
            if (page == 2 && secondPageFails) Result.failure(IOException("обрыв")) else Result.success(listOf("item$page"))
        }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        list.loadNextPage()
        advanceUntilIdle()

        // Пользователь прокрутил назад и снова дошёл до конца; сеть вернулась.
        secondPageFails = false
        list.onListEndLeft()
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 2), pages.requests)
        assertEquals(listOf("item1", "item2"), list.items.value)
        assertFalse(list.nextPageFailed.value)
    }

    @Test
    fun `подгрузка не стартует, пока идёт обновление`() = runTest {
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        val firstPage = CompletableDeferred<Unit>()
        pages.respond = { page ->
            if (page == 1) firstPage.await()
            Result.success(listOf("fresh$page"))
        }
        list.refresh()
        runCurrent()
        list.loadNextPage()
        runCurrent()

        assertEquals("подгрузка ушла на сервер посреди обновления", listOf(1, 1), pages.requests)

        firstPage.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("fresh1"), list.items.value)
        assertFalse(list.isRefreshing.value)
    }

    @Test
    fun `обновление отменяет подгрузку, и её ответ в список не попадает`() = runTest {
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        val secondPage = CompletableDeferred<Unit>()
        pages.respond = { page ->
            if (page == 2) secondPage.await()
            Result.success(listOf(if (page == 1) "fresh1" else "stale2"))
        }
        list.loadNextPage()
        runCurrent()
        list.refresh()
        runCurrent()
        secondPage.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("fresh1"), list.items.value)
        assertFalse(list.isLoading.value)
        assertFalse(list.isRefreshing.value)
    }

    @Test
    fun `исключение при загрузке не оставляет признак загрузки поднятым`() = runTest {
        pages.respond = { error("сломанный ответ") }
        val list = pagedList()

        list.loadInitial()
        advanceUntilIdle()

        assertFalse(list.isLoading.value)
        assertEquals("сломанный ответ", list.errorMessage.value)
    }

    @Test
    fun `ошибка первой загрузки показывается текстом для пользователя`() = runTest {
        pages.respond = { Result.failure(IOException("api.example.com")) }
        val list = pagedList()

        list.loadInitial()
        advanceUntilIdle()

        assertEquals("Нет связи с сервером L: api.example.com", list.errorMessage.value)
    }

    @Test
    fun `удаление элемента не пересобирает список у подписчика`() = runTest {
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        replacements.clear()

        list.removeIf { it == "item1" }

        assertTrue(list.items.value.isEmpty())
        assertTrue(replacements.isEmpty())
        assertNull(list.errorMessage.value)
    }

    /** Сервер отдаёт страницы по [pageSize] из того, что [server] вернёт на момент запроса. */
    private fun serveFrom(pageSize: Int, server: () -> List<String>) {
        pages.respond = { page -> Result.success(server().drop((page - 1) * pageSize).take(pageSize)) }
    }

    @Test
    fun `элемент, сдвинутый на следующую страницу, в списке не повторяется`() = runTest {
        var server = listOf("a", "b", "c", "d")
        serveFrom(pageSize = 2) { server }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        // На сервере появился новый элемент: вся выдача сдвинулась на одну позицию,
        // и вторая страница начинается с уже показанного «b».
        server = listOf("z") + server
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), list.items.value)
    }

    @Test
    fun `повтор узнаётся по ключу, а не по полному равенству`() = runTest {
        // Тот же элемент пришёл второй раз уже с другими полями: ключ у него прежний.
        pages.respond = { page -> Result.success(if (page == 1) listOf("a:1", "b:1") else listOf("b:2", "c:1")) }
        val list = LServerPagedList(
            scope = this,
            loadPage = pages::load,
            keyOf = { it.substringBefore(':') },
        )
        list.loadInitial()
        advanceUntilIdle()

        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("a:1", "b:1", "c:1"), list.items.value)
    }

    @Test
    fun `за страницей из одних повторов сразу идёт следующая`() = runTest {
        var server = listOf("a", "b", "c", "d", "e")
        serveFrom(pageSize = 2) { server }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        // На сервере прибавилась целая страница: вторая страница теперь — уже показанные «a» и «b».
        server = listOf("y", "z") + server
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c", "d"), list.items.value)
        assertTrue(list.hasMore)
    }

    @Test
    fun `короткая страница посреди выдачи не обрывает подгрузку`() = runTest {
        // Страницы сервера не обязаны быть ровными: конец выдачи — только пустая.
        val served = mapOf(1 to listOf("a", "b", "c"), 2 to listOf("d"), 3 to listOf("e", "f"))
        pages.respond = { page -> Result.success(served[page].orEmpty()) }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        repeat(3) {
            list.loadNextPage()
            advanceUntilIdle()
        }

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), list.items.value)
        assertFalse(list.hasMore)
    }

    @Test
    fun `удаление больше страницы возвращает подгрузку на столько же страниц`() = runTest {
        var server = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        serveFrom(pageSize = 2) { server }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        repeat(2) {
            list.loadNextPage()
            advanceUntilIdle()
        }

        // Убрано три элемента при странице в два: выдача сдвинулась на две страницы.
        server = server - setOf("a", "b", "c")
        list.removeIf { it in setOf("a", "b", "c") }
        repeat(2) {
            list.loadNextPage()
            advanceUntilIdle()
        }

        assertEquals(listOf("d", "e", "f", "g", "h"), list.items.value)
    }

    @Test
    fun `удалённый элемент не возвращается, даже если сервер ещё отдаёт его`() = runTest {
        // Сервер отстал: «a» в выдаче осталась и после удаления.
        serveFrom(pageSize = 2) { listOf("a", "b", "c") }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()

        list.removeIf { it == "a" }
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("b", "c"), list.items.value)
    }

    @Test
    fun `после удаления элемента следующая страница ничего не пропускает`() = runTest {
        var server = listOf("a", "b", "c", "d", "e", "f")
        serveFrom(pageSize = 2) { server }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        list.loadNextPage()
        advanceUntilIdle()

        // Лайк снят: список на сервере стал короче, «e» переехал на вторую страницу.
        server = server - "b"
        list.removeIf { it == "b" }
        list.loadNextPage()
        advanceUntilIdle()
        list.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("a", "c", "d", "e", "f"), list.items.value)
    }

    @Test
    fun `удаление во время подгрузки не пропускает элемент`() = runTest {
        var server = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        val thirdPage = CompletableDeferred<Unit>()
        pages.respond = { page ->
            if (page == 3) thirdPage.await()
            // Страница собрана после ожидания: сервер уже учёл удаление, и «e»
            // переехал с третьей страницы в конец второй.
            Result.success(server.drop((page - 1) * 2).take(2))
        }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        list.loadNextPage()
        advanceUntilIdle()

        list.loadNextPage()
        runCurrent()
        server = server - "a"
        list.removeIf { it == "a" }
        thirdPage.complete(Unit)
        advanceUntilIdle()
        repeat(3) {
            list.loadNextPage()
            advanceUntilIdle()
        }

        assertEquals(listOf("b", "c", "d", "e", "f", "g", "h"), list.items.value)
    }

    @Test
    fun `удаление во время подгрузки, собранной сервером до него, ничего не теряет`() = runTest {
        var server = listOf("a", "b", "c", "d", "e", "f", "g", "h")
        val thirdPage = CompletableDeferred<Unit>()
        pages.respond = { page ->
            // Страница собрана до ожидания: сервер ответил раньше, чем учёл удаление.
            val content = server.drop((page - 1) * 2).take(2)
            if (page == 3) thirdPage.await()
            Result.success(content)
        }
        val list = pagedList()
        list.loadInitial()
        advanceUntilIdle()
        list.loadNextPage()
        advanceUntilIdle()

        list.loadNextPage()
        runCurrent()
        server = server - "a"
        list.removeIf { it == "a" }
        thirdPage.complete(Unit)
        advanceUntilIdle()
        repeat(3) {
            list.loadNextPage()
            advanceUntilIdle()
        }

        assertEquals(listOf("b", "c", "d", "e", "f", "g", "h"), list.items.value)
    }
}
