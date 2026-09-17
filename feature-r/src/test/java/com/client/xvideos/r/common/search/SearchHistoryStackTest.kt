package com.client.xvideos.r.common.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchHistoryStackTest {

    private class FakeDao : IDaoSearchTemplate {
        override fun observeAllTexts(): Flow<List<String>> = emptyFlow()
        override suspend fun insertAndTrim(text: String, limit: Int) {}
        override suspend fun deleteByTexts(text: String) {}
        override suspend fun deleteAll() {}
    }

    private class TestSearchTemplate : ISearchTemplate(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        dao = FakeDao()
    )

    @Test
    fun `pushHistory добавляет элементы и игнорирует пустые и дублирующиеся`() {
        val search = TestSearchTemplate()

        search.pushHistory("apple")
        search.pushHistory("apple") // дубликат не должен добавиться
        search.pushHistory("apple ") // дубликат с пробелом на конце тоже не должен добавиться
        search.pushHistory("")      // пустой запрос игнорируется
        search.pushHistory("   ")   // пробелы игнорируются
        search.pushHistory("banana ")

        assertEquals(listOf("apple", "banana"), search.stack.toList())
    }

    @Test
    fun `popHistory извлекает предыдущий запрос отсекая текущий активный`() {
        val search = TestSearchTemplate()

        search.pushHistory("first")
        search.pushHistory("second")

        // Текущий поисковый запрос "second " с пробелом -> отмена должна вернуть "first"
        val prev1 = search.popHistory("second ")
        assertEquals("first", prev1)

        // Текущий стал "first" -> следующая отмена должна вернуть пустую строку (очистка)
        val prev2 = search.popHistory("first")
        assertEquals("", prev2)

        // Стек пуст -> дальнейшая отмена возвращает null
        val prev3 = search.popHistory("")
        assertNull(prev3)
    }

    @Test
    fun `popHistory для единственного запроса возвращает пустую строку`() {
        val search = TestSearchTemplate()

        search.pushHistory("only")
        val prev = search.popHistory("only")
        assertEquals("", prev)
    }

    @Test
    fun `pushHistory ограничивает максимальный размер стека 50 элементами`() {
        val search = TestSearchTemplate()

        for (i in 1..60) {
            search.pushHistory("query_$i")
        }

        assertEquals(ISearchTemplate.MAX_STACK_SIZE, search.stack.size)
        // Первые 10 старых записей вытеснены, самый старый - query_11, самый свежий - query_60
        assertEquals("query_11", search.stack.first())
        assertEquals("query_60", search.stack.last())
    }
}
