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
}
