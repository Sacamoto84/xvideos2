package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.XRubric
import org.junit.Assert.assertEquals
import org.junit.Test

/** Меню категорий сайта: только ссылки `/c/<slug>-<id>` из `#main-cats-sub-list`. */
class ParserRubricMenuTest {

    /** Разметка сокращена с живой страницы: те же id и классы, подписи нейтральные. */
    private val menu = """
        <ul class="sub-list" id="main-cats-sub-list">
          <li class="dyn"><a href="/lang/one">Язык</a></li>
          <li class="dyntop-cat top-cat-2"><a href="/c/Cat_B-12">Категория Б</a></li>
          <li class="dyn"><a href="/gay?fmc=1">Ориентация</a></li>
          <li class="dyntop-cat top-cat-1"><a href="/c/A-b_c-7">Категория &amp; А</a></li>
          <li class="dyntop-cat top-cat-3"><a href="/c/Cat_B-12">Дубль</a></li>
          <li class="dyn"><a href="/tags">Все</a></li>
        </ul>
        <a href="/c/Outside-99">Вне меню</a>
    """.trimIndent()

    @Test
    fun `из меню берутся только категории, в порядке сайта, без дублей`() {
        assertEquals(
            listOf(XRubric(12, "Cat_B", "Категория Б"), XRubric(7, "A-b_c", "Категория & А")),
            parserRubricMenu(menu),
        )
    }

    @Test
    fun `id — число после последнего дефиса`() {
        val rubric = parserRubricMenu(menu)[1]

        assertEquals(7, rubric.id)
        assertEquals("A-b_c", rubric.slug)
    }

    @Test
    fun `без меню — пустой список`() {
        assertEquals(emptyList<XRubric>(), parserRubricMenu("<html></html>"))
    }
}
