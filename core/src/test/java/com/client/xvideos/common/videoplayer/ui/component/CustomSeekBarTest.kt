package com.client.xvideos.common.videoplayer.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomSeekBarTest {

    @Test
    fun `calculateCustomSeekBarProgressX calculates proportional x for normal values`() {
        val result = calculateCustomSeekBarProgressX(
            localProgress = 50f,
            maxProgress = 100f,
            trackWidthPx = 800f
        )
        assertEquals(400f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarProgressX returns 0 when progress is 0`() {
        val result = calculateCustomSeekBarProgressX(
            localProgress = 0f,
            maxProgress = 100f,
            trackWidthPx = 800f
        )
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarProgressX clamps to trackWidth when progress exceeds maxProgress`() {
        val result = calculateCustomSeekBarProgressX(
            localProgress = 150f,
            maxProgress = 100f,
            trackWidthPx = 800f
        )
        assertEquals(800f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarProgressX clamps negative progress to 0`() {
        val result = calculateCustomSeekBarProgressX(
            localProgress = -20f,
            maxProgress = 100f,
            trackWidthPx = 800f
        )
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarProgressX returns 0 for non-finite or zero values`() {
        assertEquals(0f, calculateCustomSeekBarProgressX(Float.NaN, 100f, 800f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, Float.NaN, 800f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, 100f, Float.NaN), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(Float.POSITIVE_INFINITY, 100f, 800f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, 0f, 800f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, -10f, 800f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, 100f, 0f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarProgressX(50f, 100f, -50f), 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarSeekPosition calculates proportional seek position`() {
        val result = calculateCustomSeekBarSeekPosition(
            offsetX = 200f,
            trackWidthPx = 800f,
            maxProgress = 100f
        )
        assertEquals(25f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarSeekPosition clamps out of bounds offsets`() {
        assertEquals(0f, calculateCustomSeekBarSeekPosition(-50f, 800f, 100f), 0.001f)
        assertEquals(100f, calculateCustomSeekBarSeekPosition(950f, 800f, 100f), 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarSeekPosition returns 0 for non-finite or invalid values`() {
        assertEquals(0f, calculateCustomSeekBarSeekPosition(Float.NaN, 800f, 100f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarSeekPosition(200f, 0f, 100f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarSeekPosition(200f, 800f, 0f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarSeekPosition(200f, Float.POSITIVE_INFINITY, 100f), 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarDragDelta adds drag delta proportionally`() {
        val result = calculateCustomSeekBarDragDelta(
            dragAmountX = 80f,
            trackWidthPx = 800f,
            maxProgress = 100f,
            currentProgress = 20f
        )
        // 80 / 800 * 100 = 10 -> 20 + 10 = 30
        assertEquals(30f, result, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarDragDelta handles negative delta and clamps to bounds`() {
        val result = calculateCustomSeekBarDragDelta(
            dragAmountX = -200f,
            trackWidthPx = 800f,
            maxProgress = 100f,
            currentProgress = 10f
        )
        assertEquals(0f, result, 0.001f)

        val overflowResult = calculateCustomSeekBarDragDelta(
            dragAmountX = 800f,
            trackWidthPx = 800f,
            maxProgress = 100f,
            currentProgress = 50f
        )
        assertEquals(100f, overflowResult, 0.001f)
    }

    @Test
    fun `calculateCustomSeekBarDragDelta safely handles non-finite values`() {
        assertEquals(20f, calculateCustomSeekBarDragDelta(Float.NaN, 800f, 100f, 20f), 0.001f)
        assertEquals(20f, calculateCustomSeekBarDragDelta(50f, 0f, 100f, 20f), 0.001f)
        assertEquals(0f, calculateCustomSeekBarDragDelta(50f, 800f, 100f, Float.NaN), 0.001f)
    }
}
