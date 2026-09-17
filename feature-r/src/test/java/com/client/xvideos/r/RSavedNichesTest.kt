package com.client.xvideos.r

import com.client.xvideos.r.model.NichesInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RSavedNichesTest {

    @Test
    fun `идемпотентное добавление ниши заменяет существующую запись с тем же id`() {
        val initial = NichesInfo(id = "niche_1", name = "Old Name", thumbnail = "https://example.com/1.jpg")
        val list = mutableListOf(initial)

        val updated = NichesInfo(id = "niche_1", name = "New Name", thumbnail = "https://example.com/2.jpg")

        // Семантика add в R_Saved_Niches:
        list.removeAll { it.id == updated.id }
        list.add(updated)

        assertEquals(1, list.size)
        assertEquals("New Name", list.first().name)
        assertEquals("https://example.com/2.jpg", list.first().thumbnail)
    }

    @Test
    fun `удаление ниши по id удаляет только целевую запись`() {
        val niche1 = NichesInfo(id = "1", name = "Cosplay")
        val niche2 = NichesInfo(id = "2", name = "Anime")
        val niche3 = NichesInfo(id = "3", name = "Gaming")
        val list = mutableListOf(niche1, niche2, niche3)

        // Семантика remove в R_Saved_Niches:
        val removed = list.removeAll { it.id == "2" }

        assertTrue(removed)
        assertEquals(2, list.size)
        assertEquals(listOf("1", "3"), list.map { it.id })
    }

    @Test
    fun `удаление отсутствующей ниши возвращает false и не повреждает список`() {
        val niche1 = NichesInfo(id = "1", name = "Cosplay")
        val list = mutableListOf(niche1)

        val removed = list.removeAll { it.id == "99" }

        assertFalse(removed)
        assertEquals(1, list.size)
        assertEquals("1", list.first().id)
    }

    @Test
    fun `поиск ниш фильтрует регистронезависимо по названию`() {
        val niches = listOf(
            NichesInfo(id = "1", name = "Cosplay Girls"),
            NichesInfo(id = "2", name = "Anime Art"),
            NichesInfo(id = "3", name = "Cyberpunk Gaming")
        )

        fun searchNiches(query: String): List<NichesInfo> {
            val q = query.trim().lowercase()
            return if (q.isEmpty()) niches else niches.filter { it.name.lowercase().contains(q) }
        }

        val resultCosplay = searchNiches("cosplay")
        assertEquals(1, resultCosplay.size)
        assertEquals("Cosplay Girls", resultCosplay.first().name)

        val resultUpper = searchNiches("ANIME")
        assertEquals(1, resultUpper.size)
        assertEquals("Anime Art", resultUpper.first().name)

        val resultEmpty = searchNiches("nonexistent")
        assertTrue(resultEmpty.isEmpty())
    }
}
