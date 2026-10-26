package com.client.xvideos.xlive.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Плейлисты здесь синтетические: устройство как у эфира, адреса выдуманные. */
class XLivePlaylistsTest {

    private val masterUrl = "https://edge.test/live/abc/master.m3u8?t=token"

    private val master = """
        #EXTM3U
        #EXT-X-VERSION:6
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac_lo",NAME="audio",DEFAULT=YES,URI="audio_lo.m3u8?t=token"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aac_hi",NAME="audio",DEFAULT=YES,URI="audio_hi.m3u8?t=token"
        #EXT-X-STREAM-INF:BANDWIDTH=896000,RESOLUTION=640x360,CODECS="avc1.42c01e,mp4a.40.2",AUDIO="aac_lo"
        v360.m3u8?t=token
        #EXT-X-STREAM-INF:BANDWIDTH=1696000,RESOLUTION=960x540,CODECS="avc1.4d401f,mp4a.40.2",AUDIO="aac_lo"
        v540.m3u8?t=token
        #EXT-X-STREAM-INF:BANDWIDTH=7128000,RESOLUTION=1920x1080,CODECS="avc1.64002a,mp4a.40.2",AUDIO="aac_hi"
        v1080.m3u8?t=token
        #EXT-X-STREAM-INF:BANDWIDTH=4096000,RESOLUTION=1280x720,CODECS="avc1.4d4028,mp4a.40.2",AUDIO="aac_hi"
        https://other.test/v720.m3u8?t=token
    """.trimIndent()

    private val media = """
        #EXTM3U
        #EXT-X-VERSION:9
        #EXT-X-TARGETDURATION:2
        #EXT-X-SERVER-CONTROL:CAN-BLOCK-RELOAD=YES,PART-HOLD-BACK=2.43
        #EXT-X-PART-INF:PART-TARGET=0.799
        #EXT-X-MEDIA-SEQUENCE:100
        #EXT-X-MAP:URI="init_v.m4s?t=token"
        #EXT-X-PROGRAM-DATE-TIME:2026-10-26T06:00:00.000Z
        #EXTINF:1.633,
        seg_v_100.m4s?t=token
        #EXT-X-PART:DURATION=0.799,URI="part_v_101_0.m4s?t=token",INDEPENDENT=YES
        #EXTINF:1.624,
        seg_v_101.m4s?t=token
        #EXTINF:1.633,
        seg_v_102.m4s?t=token
        #EXT-X-PROGRAM-DATE-TIME:2026-10-26T06:00:04.890Z
        #EXTINF:1.624,
        seg_v_103.m4s?t=token
        #EXT-X-PART:DURATION=0.799,URI="part_v_104_0.m4s?t=token",INDEPENDENT=YES
        #EXT-X-PRELOAD-HINT:TYPE=PART,URI="part_v_104_1.m4s?t=token"
        #EXT-X-RENDITION-REPORT:URI="v540.m3u8",LAST-MSN=103,LAST-PART=0
    """.trimIndent()

    @Test
    fun `лучший вариант — с наибольшей полосой и звуком своей группы`() {
        val best = parseBestVariant(master, masterUrl)!!

        assertEquals("https://edge.test/live/abc/v1080.m3u8?t=token", best.videoUrl)
        assertEquals("https://edge.test/live/abc/audio_hi.m3u8?t=token", best.audioUrl)
        assertEquals(7_128_000L, best.bandwidth)
        assertEquals(1920, best.width)
        assertEquals(1080, best.height)
    }

    @Test
    fun `вариант без группы звука — звука отдельно нет`() {
        val single = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1000,RESOLUTION=640x360
            only.m3u8
        """.trimIndent()

        val best = parseBestVariant(single, masterUrl)!!

        assertEquals("https://edge.test/live/abc/only.m3u8", best.videoUrl)
        assertNull(best.audioUrl)
    }

    @Test
    fun `мастер без вариантов — null`() {
        assertNull(parseBestVariant("#EXTM3U\n", masterUrl))
    }

    @Test
    fun `медиаплейлист — init и полные сегменты с номерами, части не в счёт`() {
        val url = "https://edge.test/live/abc/v1080.m3u8?t=token"

        val playlist = parseMediaPlaylist(media, url)!!

        assertEquals("https://edge.test/live/abc/init_v.m4s?t=token", playlist.initUrl)
        assertEquals(listOf(100L, 101L, 102L, 103L), playlist.segments.map { it.msn })
        assertEquals("https://edge.test/live/abc/seg_v_102.m4s?t=token", playlist.segments[2].url)
        assertEquals(1.624, playlist.segments[1].durationSec, 1e-9)
        assertEquals(103L, playlist.lastMsn)
    }

    @Test
    fun `не плейлист — null`() {
        assertNull(parseMediaPlaylist("<html>403</html>", "https://edge.test/v.m3u8"))
    }

    @Test
    fun `блокирующее перечитывание добавляет номер, сохраняя токен`() {
        assertEquals(
            "https://edge.test/v.m3u8?t=abc&_HLS_msn=104",
            blockingReloadUrl("https://edge.test/v.m3u8?t=abc", 104),
        )
        assertEquals("https://edge.test/v.m3u8?_HLS_msn=7", blockingReloadUrl("https://edge.test/v.m3u8", 7))
    }
}
