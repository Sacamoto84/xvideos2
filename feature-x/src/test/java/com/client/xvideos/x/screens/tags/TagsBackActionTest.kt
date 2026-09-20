package com.client.xvideos.x.screens.tags

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Проверка действия возврата («Назад») на экране тегов:
 * Немедленный выход из экрана (POP) без удержания на странице или позиции скролла.
 */
class TagsBackActionTest {

    @Test
    fun `действие возврата немедленно закрывает экран`() {
        assertEquals(
            TagsBackAction.POP,
            resolveTagsBackAction()
        )
    }
}
