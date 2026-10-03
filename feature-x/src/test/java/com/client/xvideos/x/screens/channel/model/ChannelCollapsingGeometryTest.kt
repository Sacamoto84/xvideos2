package com.client.xvideos.x.screens.channel.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelCollapsingGeometryTest {

    private val headerHeight = 600
    private val stickyBarHeight = 80
    private val topInset = 100

    private fun geometry(offset: Float, inset: Int = topInset) = channelCollapsingGeometry(
        headerOffsetPx = offset,
        headerHeight = headerHeight,
        stickyBarHeight = stickyBarHeight,
        topInsetPx = inset,
    )

    @Test
    fun `раскрытая шапка начинается от верха экрана и не закрыта плашкой`() {
        val g = geometry(offset = 0f)
        assertEquals(0, g.headerY)
        assertEquals(600, g.stickyY)
        assertEquals(680, g.pagerY)
        assertEquals(0f, g.coverAlpha, 0f)
    }

    @Test
    fun `в конце пути схлопывания панель стоит под вырезом, плашка непрозрачна`() {
        // Путь схлопывания — высота шапки без выреза.
        val g = geometry(offset = -(headerHeight - topInset).toFloat())
        assertEquals(-500, g.headerY)
        assertEquals(topInset, g.stickyY)
        assertEquals(topInset + stickyBarHeight, g.pagerY)
        assertEquals(1f, g.coverAlpha, 0f)
    }

    @Test
    fun `плашка проявляется на последнем отрезке длиной в вырез`() {
        assertEquals(0f, geometry(offset = -400f).coverAlpha, 0f) // панель в двух вырезах от верха
        assertEquals(0.5f, geometry(offset = -450f).coverAlpha, 0.0001f)
        assertEquals(1f, geometry(offset = -600f).coverAlpha, 0f) // дальше панель не поднимается
        assertEquals(topInset, geometry(offset = -600f).stickyY)
    }

    @Test
    fun `без выреза плашки нет, панель прилипает к верху экрана`() {
        val g = geometry(offset = -600f, inset = 0)
        assertEquals(0, g.stickyY)
        assertEquals(stickyBarHeight, g.pagerY)
        assertEquals(0f, g.coverAlpha, 0f)
    }
}
