package com.client.xvideos.x.feature.saved

import com.client.xvideos.common.videoplayer.model.MediaClip
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Превью сохранённого ролика — 5 отрезков по секунде из самого файла:
 * начала в 10, 30, 50, 70 и 90% длительности. Короткий файл или файл без
 * длительности играет целиком.
 */
class LocalPreviewClipsTest {

    @Test
    fun `пять отрезков по секунде в 10–90 процентах`() {
        assertEquals(
            listOf(
                MediaClip(10_000, 11_000),
                MediaClip(30_000, 31_000),
                MediaClip(50_000, 51_000),
                MediaClip(70_000, 71_000),
                MediaClip(90_000, 91_000),
            ),
            localPreviewClips(100_000L)
        )
    }

    @Test
    fun `конец отрезка не дальше длительности`() {
        assertEquals(MediaClip(4_500, 5_000), localPreviewClips(5_000L).last())
    }

    @Test
    fun `короткий файл и неизвестная длительность — весь файл`() {
        assertEquals(emptyList<MediaClip>(), localPreviewClips(4_999L))
        assertEquals(emptyList<MediaClip>(), localPreviewClips(null))
        assertEquals(emptyList<MediaClip>(), localPreviewClips(0L))
    }
}
