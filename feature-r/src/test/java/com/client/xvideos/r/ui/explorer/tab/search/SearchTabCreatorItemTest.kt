package com.client.xvideos.r.ui.explorer.tab.search

import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SearchTabCreatorItemTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos -> oos.writeObject(value) }
            baos.toByteArray()
        }
        return ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois -> ois.readObject() as T }
        }
    }

    @Test
    fun `SearchTab singleton serializes and deserializes properly with stable key`() {
        val restored = roundTrip(SearchTab)
        assertEquals("RedSearchTab", restored.key)
        assertEquals(SearchTab, restored)
    }

    @Test
    fun `creator handle extracts cleanly with or without at prefix`() {
        val withPrefix = SearchItemCreatorsResponse(text = "@supermodel", name = "Super Model")
        val handle1 = withPrefix.text.removePrefix("@").ifBlank { withPrefix.name }
        assertEquals("supermodel", handle1)

        val withoutPrefix = SearchItemCreatorsResponse(text = "artist123", name = "Artist")
        val handle2 = withoutPrefix.text.removePrefix("@").ifBlank { withoutPrefix.name }
        assertEquals("artist123", handle2)

        val emptyText = SearchItemCreatorsResponse(text = "@", name = "FallbackName")
        val handle3 = emptyText.text.removePrefix("@").ifBlank { emptyText.name }
        assertEquals("FallbackName", handle3)
    }

    @Test
    fun `followers formatting produces readable counts`() {
        assertEquals("999", 999L.toPrettyCount())
        assertEquals("1.0K", 1000L.toPrettyCount())
        assertEquals("15.5K", 15500L.toPrettyCount())
        assertEquals("1.2M", 1200000L.toPrettyCount())
    }
}
