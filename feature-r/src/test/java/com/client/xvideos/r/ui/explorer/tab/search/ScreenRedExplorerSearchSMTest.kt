package com.client.xvideos.r.ui.explorer.tab.search

import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Поиск авторов R: сбой запроса не должен оставлять под новым текстом авторов
 * прежнего запроса. Раньше список не очищался, а ошибка шла только в лог.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenRedExplorerSearchSMTest {

    private val dispatcher = StandardTestDispatcher()
    private val notices = mutableListOf<String>()
    private var respond: (query: String) -> Result<List<SearchItemCreatorsResponse>> = { query ->
        Result.success(listOf(SearchItemCreatorsResponse(name = "author of $query")))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun screenModel() = ScreenRedExplorerSearchSM(
        searchCreators = { query -> respond(query) },
        notifyFailure = { notices += it },
    )

    @Test
    fun `сбой поиска убирает авторов прежнего запроса и сообщает об ошибке`() = runTest(dispatcher) {
        val sm = screenModel()
        sm.updateSearchText("abc")
        advanceUntilIdle()
        assertEquals(listOf("author of abc"), sm.creatorsList.map { it.name })

        respond = { Result.failure(IOException("обрыв")) }
        sm.updateSearchText("xyz")
        advanceUntilIdle()

        assertTrue("под новым запросом остались авторы прежнего", sm.creatorsList.isEmpty())
        assertEquals(listOf("Поиск авторов не удался: Нет связи с сервером R"), notices)
        assertFalse(sm.isLoading.value)
    }

    @Test
    fun `пустой запрос очищает список без обращения к серверу`() = runTest(dispatcher) {
        val sm = screenModel()
        sm.updateSearchText("abc")
        advanceUntilIdle()

        respond = { error("сервер не должен вызываться") }
        sm.updateSearchText("   ")
        advanceUntilIdle()

        assertTrue(sm.creatorsList.isEmpty())
        assertTrue(notices.isEmpty())
    }
}
