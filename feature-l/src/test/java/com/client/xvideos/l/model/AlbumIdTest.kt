package com.client.xvideos.l.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumIdTest {

    @Test
    fun `id альбома возвращается как есть`() {
        assertEquals("123", PicsDetails(album = "123").albumIdOrNull)
    }

    @Test
    fun `нет альбома, пустая строка и строка null из старых данных — альбома нет`() {
        assertNull(PicsDetails(album = null).albumIdOrNull)
        assertNull(PicsDetails(album = " ").albumIdOrNull)
        assertNull(PicsDetails(album = "null").albumIdOrNull)
        assertNull(PicsDetails().albumIdOrNull)
    }
}
