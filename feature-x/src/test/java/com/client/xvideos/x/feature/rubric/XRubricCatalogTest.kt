package com.client.xvideos.x.feature.rubric

import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

/** Каталог категорий: адрес меню ориентации, сортировка, кеш на процесс. */
class XRubricCatalogTest {

    private fun menuOf(vararg titles: String) = titles.mapIndexed { i, title ->
        """<li class="dyntop-cat"><a href="/c/R$i-$i">$title</a></li>"""
    }.joinToString("", """<ul id="main-cats-sub-list">""", "</ul>")

    @Test
    fun `адрес меню — смена ориентации из свежей сессии`() {
        assertEquals("$urlStart/switch-sexual-orientation/straight/straight", buildRubricMenuUrl(XCategory.STRAIGHT))
        assertEquals("$urlStart/switch-sexual-orientation/gay/straight", buildRubricMenuUrl(XCategory.GAY))
        assertEquals("$urlStart/switch-sexual-orientation/shemale/straight", buildRubricMenuUrl(XCategory.TRANS))
    }

    @Test
    fun `список — по алфавиту`() {
        // Только кириллица: порядок латиницы относительно кириллицы на JVM и Android разный.
        val catalog = XRubricCatalog { menuOf("яблоко", "Арбуз", "банан") }

        val titles = runBlocking { catalog.load(XCategory.STRAIGHT) }.map { it.title }

        assertEquals(listOf("Арбуз", "банан", "яблоко"), titles)
    }

    @Test
    fun `повторная загрузка той же ориентации не идёт в сеть`() {
        var calls = 0
        val catalog = XRubricCatalog { calls++; menuOf("Арбуз") }

        runBlocking {
            catalog.load(XCategory.STRAIGHT)
            catalog.load(XCategory.STRAIGHT)
        }

        assertEquals(1, calls)
        assertNotNull(catalog.cached(XCategory.STRAIGHT))
    }

    @Test
    fun `у каждой ориентации свой запрос`() {
        val urls = mutableListOf<String>()
        val catalog = XRubricCatalog { url -> urls += url; menuOf("Арбуз") }

        runBlocking {
            catalog.load(XCategory.GAY)
            catalog.load(XCategory.TRANS)
        }

        assertEquals(listOf(buildRubricMenuUrl(XCategory.GAY), buildRubricMenuUrl(XCategory.TRANS)), urls)
    }

    @Test
    fun `ошибка не кешируется`() {
        var offline = true
        val catalog = XRubricCatalog { if (offline) throw IOException("нет сети") else menuOf("Арбуз") }

        assertThrows(IOException::class.java) { runBlocking { catalog.load(XCategory.STRAIGHT) } }
        assertNull(catalog.cached(XCategory.STRAIGHT))

        offline = false
        assertEquals(1, runBlocking { catalog.load(XCategory.STRAIGHT) }.size)
    }

    @Test
    fun `пустое меню — IOException`() {
        val catalog = XRubricCatalog { "<html></html>" }

        assertThrows(IOException::class.java) { runBlocking { catalog.load(XCategory.STRAIGHT) } }
        assertNull(catalog.cached(XCategory.STRAIGHT))
    }
}
