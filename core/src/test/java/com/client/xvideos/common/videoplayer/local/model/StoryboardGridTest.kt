package com.client.xvideos.common.videoplayer.local.model

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Раскадровка X — сетка 10×10, кадр на каждый процент длины ролика, по строкам
 * слева направо. Так же номер кадра считает плеер сайта.
 */
class StoryboardGridTest {

    @Test
    fun `номер кадра — процент длины`() {
        assertEquals(0, storyboardFrameIndex(0f, 100))
        assertEquals(0, storyboardFrameIndex(0.99f, 100))
        assertEquals(1, storyboardFrameIndex(1f, 100))
        assertEquals(50, storyboardFrameIndex(50.5f, 100))
        assertEquals(99, storyboardFrameIndex(99.99f, 100))
    }

    @Test
    fun `номер кадра за краями и без длительности`() {
        assertEquals(99, storyboardFrameIndex(100f, 100))
        assertEquals(99, storyboardFrameIndex(150f, 100))
        assertEquals(0, storyboardFrameIndex(-5f, 100))
        assertEquals(0, storyboardFrameIndex(Float.NaN, 100))
        assertEquals(0, storyboardFrameIndex(30f, 0))
    }

    @Test
    fun `прямоугольник кадра в сетке 10x10`() {
        assertEquals(IntRect(0, 0, 192, 108), storyboardFrameRect(0, 1920, 1080))
        assertEquals(IntRect(1728, 0, 1920, 108), storyboardFrameRect(9, 1920, 1080))
        assertEquals(IntRect(0, 108, 192, 216), storyboardFrameRect(10, 1920, 1080))
        assertEquals(IntRect(1728, 972, 1920, 1080), storyboardFrameRect(99, 1920, 1080))
        assertEquals(IntRect(1728, 972, 1920, 1080), storyboardFrameRect(150, 1920, 1080))
    }

    @Test
    fun `вырожденная картинка кадра не даёт`() {
        assertNull(storyboardFrameRect(5, 9, 9))
        assertNull(storyboardFrameRect(0, 0, 0))
    }

    @Test
    fun `якорь — доля длины на полосе в координатах оверлея`() {
        assertEquals(150f, seekPreviewAnchorX(50f, 100, 100f, 200f, 50f))
        assertEquals(50f, seekPreviewAnchorX(30f, 0, 100f, 200f, 50f))
        assertEquals(250f, seekPreviewAnchorX(500f, 100, 100f, 200f, 50f))
    }

    @Test
    fun `кадр A упирается в края`() {
        assertEquals(420f, seekPreviewFollowLeft(500f, 160f, 1000f, 8f))
        assertEquals(8f, seekPreviewFollowLeft(20f, 160f, 1000f, 8f))
        assertEquals(832f, seekPreviewFollowLeft(990f, 160f, 1000f, 8f))
        assertEquals(-5f, seekPreviewFollowLeft(100f, 160f, 150f, 8f))
    }
}
