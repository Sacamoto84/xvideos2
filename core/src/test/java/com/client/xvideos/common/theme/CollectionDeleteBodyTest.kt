package com.client.xvideos.common.theme

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Текст окна удаления коллекции. Раньше оба раздела писали «Удалить «имя» из
 * коллекции»: так читается удаление одной записи, хотя коллекция удаляется
 * целиком, со всем содержимым.
 */
class CollectionDeleteBodyTest {

    @Test
    fun `текст говорит об удалении всей коллекции`() {
        assertEquals(
            "Коллекция «Избранное» будет удалена вместе со всем содержимым",
            collectionDeleteBody("Избранное").text,
        )
    }

    @Test
    fun `жирным выделено только имя коллекции`() {
        val body = collectionDeleteBody("Избранное")

        val bold = body.spanStyles.single()

        assertEquals(FontWeight.Bold, bold.item.fontWeight)
        assertEquals("Избранное", body.text.substring(bold.start, bold.end))
    }
}
