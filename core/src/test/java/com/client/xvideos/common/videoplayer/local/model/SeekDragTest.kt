package com.client.xvideos.common.videoplayer.local.model

import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Панель управления может уйти из композиции прямо под пальцем (автоскрытие
 * сработало в тот же кадр, что и захват ползунка). `detectDragGestures` тогда
 * не зовёт `onDragCancel`: превью залипало, перемотка терялась, время замирало
 * (финальное ревью плана 2026-10-21-x-seek-preview, I1).
 */
class SeekDragTest {

    @Test
    fun `брошенное перетаскивание завершается перемоткой и снимает превью`() {
        val host = MediaPlayerHost()
        val scrubs = mutableListOf<SeekScrub?>()
        host.isSliding = true

        host.finishSeekDragIfActive(42f) { scrubs += it }

        assertEquals(42f, host.seekRequest?.seconds)
        assertFalse(host.isSliding)
        assertEquals(listOf<SeekScrub?>(null), scrubs)
        host.dispose()
    }

    @Test
    fun `без перетаскивания уход панели ничего не делает`() {
        val host = MediaPlayerHost()
        val scrubs = mutableListOf<SeekScrub?>()

        host.finishSeekDragIfActive(42f) { scrubs += it }

        assertNull(host.seekRequest)
        assertTrue(scrubs.isEmpty())
        host.dispose()
    }

    @Test
    fun `отпускание пальца — одна перемотка и снятие превью`() {
        val host = MediaPlayerHost()
        val scrubs = mutableListOf<SeekScrub?>()
        host.isSliding = true

        host.finishSeekDrag(17f) { scrubs += it }

        assertEquals(17f, host.seekRequest?.seconds)
        assertFalse(host.isSliding)
        assertEquals(listOf<SeekScrub?>(null), scrubs)
        host.dispose()
    }
}
