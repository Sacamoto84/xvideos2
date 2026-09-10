package com.client.xvideos.common.util

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplaceWithTest {

    @Test
    fun `список получает новое содержимое`() {
        val list = mutableStateListOf("a", "b", "c")

        list.replaceWith(listOf("x", "y"))

        assertEquals(listOf("x", "y"), list.toList())
    }

    @Test
    fun `пустой список очищает содержимое`() {
        val list = mutableStateListOf("a", "b")

        list.replaceWith(emptyList())

        assertEquals(emptyList<String>(), list.toList())
    }

    /**
     * Главное, ради чего написан хелпер: наблюдатель не должен увидеть
     * промежуточное пустое состояние между clear() и addAll().
     */
    @Test
    fun `наблюдатель не видит промежуточного пустого списка`() {
        val list = mutableStateListOf("a", "b", "c")
        val seen = mutableListOf<List<String>>()

        val observer = Snapshot.registerApplyObserver { changed, _ ->
            if (list in changed) seen.add(list.toList())
        }
        try {
            list.replaceWith(listOf("x", "y"))
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }

        // Ровно одна публикация, и сразу с итоговым содержимым.
        assertEquals(listOf(listOf("x", "y")), seen)
    }

    @Test
    fun `замена из копии самого списка не теряет элементы`() {
        val list = mutableStateListOf("a", "b", "c")

        list.replaceWith(list.toList().reversed())

        assertEquals(listOf("c", "b", "a"), list.toList())
    }
}
