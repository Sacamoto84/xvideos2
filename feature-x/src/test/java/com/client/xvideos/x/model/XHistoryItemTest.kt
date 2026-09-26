package com.client.xvideos.x.model

import com.client.xvideos.common.json.AppJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XHistoryItemTest {

    @Test
    fun `сериализация и десериализация корректно сохраняют все поля`() {
        val original = XHistoryItem(
            item = ItemsX(
                id = 12345L,
                title = "Тестовое видео",
                duration = "10 мин.",
                views = "50K",
                channel = "TestChannel",
                previewImage = "https://example.com/thumb.jpg",
                href = "/video12345/test"
            ),
            lastPositionMs = 150_000L,
            totalDurationMs = 600_000L,
            updatedAt = 1_700_000_000_000L
        )

        val json = AppJson.encodeToString(XHistoryItem.serializer(), original)
        val decoded = AppJson.decodeFromString(XHistoryItem.serializer(), json)

        assertEquals(original, decoded)
    }

    @Test
    fun `десериализация пустого JSON возвращает значения по умолчанию`() {
        val decoded = AppJson.decodeFromString(XHistoryItem.serializer(), "{}")
        assertEquals(0L, decoded.lastPositionMs)
        assertEquals(0L, decoded.totalDurationMs)
        assertEquals(0L, decoded.updatedAt)
        assertEquals(0L, decoded.item.id)
    }

    @Test
    fun `isEligibleForResume возвращает false для роликов короче 2 минут`() {
        val shortVideo = XHistoryItem(
            lastPositionMs = 30_000L,
            totalDurationMs = 119_000L // Меньше 120 000 мс (2 мин)
        )
        assertFalse(shortVideo.isEligibleForResume)
    }

    @Test
    fun `isEligibleForResume возвращает false если просмотрено менее 5 секунд`() {
        val barelyStarted = XHistoryItem(
            lastPositionMs = 4_000L, // Меньше 5 000 мс
            totalDurationMs = 300_000L
        )
        assertFalse(barelyStarted.isEligibleForResume)
    }

    @Test
    fun `isEligibleForResume возвращает false если ролик досмотрен до 95 процентов и более`() {
        val almostDone = XHistoryItem(
            lastPositionMs = 286_000L, // 286 / 300 = 95.3%
            totalDurationMs = 300_000L
        )
        assertFalse(almostDone.isEligibleForResume)
    }

    @Test
    fun `isEligibleForResume возвращает true для подходящих роликов`() {
        val eligible = XHistoryItem(
            lastPositionMs = 60_000L, // 1 минута
            totalDurationMs = 300_000L // 5 минут (>= 2 мин)
        )
        assertTrue(eligible.isEligibleForResume)
    }

    @Test
    fun `progressFraction корректно вычисляет долю просмотренного`() {
        val item = XHistoryItem(
            lastPositionMs = 150_000L,
            totalDurationMs = 300_000L
        )
        assertEquals(0.5f, item.progressFraction, 0.001f)

        val zeroDuration = XHistoryItem(lastPositionMs = 100L, totalDurationMs = 0L)
        assertEquals(0f, zeroDuration.progressFraction, 0.001f)

        val completedItem = XHistoryItem(isCompleted = true, lastPositionMs = 0L, totalDurationMs = 300_000L)
        assertEquals(1f, completedItem.progressFraction, 0.001f)
    }

    @Test
    fun `isEligibleForResume возвращает false если ролик помечен как isCompleted`() {
        val completed = XHistoryItem(
            isCompleted = true,
            lastPositionMs = 60_000L,
            totalDurationMs = 300_000L
        )
        assertFalse(completed.isEligibleForResume)
    }

    @Test
    fun `десериализация старого JSON без isCompleted выставляет false по умолчанию`() {
        val oldJson = """{"lastPositionMs":0,"totalDurationMs":300000}"""
        val decoded = AppJson.decodeFromString(XHistoryItem.serializer(), oldJson)
        assertFalse(decoded.isCompleted)
    }

    @Test
    fun `hasProgress and hasTotalDuration accurately reflect millisecond fields`() {
        val emptyItem = XHistoryItem.EMPTY
        assertFalse(emptyItem.hasProgress)
        assertFalse(emptyItem.hasTotalDuration)

        val itemWithDuration = XHistoryItem(totalDurationMs = 120_000L)
        assertFalse(itemWithDuration.hasProgress)
        assertTrue(itemWithDuration.hasTotalDuration)

        val itemWithBoth = XHistoryItem(lastPositionMs = 50_000L, totalDurationMs = 120_000L)
        assertTrue(itemWithBoth.hasProgress)
        assertTrue(itemWithBoth.hasTotalDuration)
    }
}
