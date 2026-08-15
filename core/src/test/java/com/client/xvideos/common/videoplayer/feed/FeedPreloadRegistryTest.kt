package com.client.xvideos.common.videoplayer.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPreloadRegistryTest {

    @Test
    fun `первый track просит добавить элемент`() {
        val registry = FeedPreloadRegistry<String>()
        assertEquals(PreloadAction.Add("a"), registry.track(1, "a"))
    }

    @Test
    fun `повторный track тем же элементом не просит ничего`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        assertEquals(PreloadAction.None, registry.track(1, "a"))
    }

    @Test
    fun `track другим элементом на том же индексе просит заменить`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        assertEquals(PreloadAction.Replace("a", "b"), registry.track(1, "b"))
    }

    @Test
    fun `track снимает индекс с ожидания`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.track(1, "a")
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `forget отдаёт отслеживаемый элемент и чистит ожидание`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.track(1, "a")
        assertEquals("a", registry.forget(1))
        assertNull(registry.forget(1))
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `forget неизвестного индекса ничего не отдаёт`() {
        val registry = FeedPreloadRegistry<String>()
        assertNull(registry.forget(42))
    }

    @Test
    fun `pendingIndices отдаёт копию и переживает правку реестра во время обхода`() {
        val registry = FeedPreloadRegistry<String>()
        registry.markPending(1)
        registry.markPending(2)
        val snapshot = registry.pendingIndices()
        snapshot.forEach { registry.track(it, "item$it") }
        assertEquals(listOf(1, 2), snapshot.sorted())
        assertTrue(registry.pendingIndices().isEmpty())
    }

    @Test
    fun `clear чистит и отслеживаемое и ожидающее`() {
        val registry = FeedPreloadRegistry<String>()
        registry.track(1, "a")
        registry.markPending(2)
        registry.clear()
        assertNull(registry.forget(1))
        assertTrue(registry.pendingIndices().isEmpty())
    }
}
