package com.client.xvideos.common.videoplayer.util

import io.ktor.http.HttpHeaders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запрос плейлиста ради списка качеств: заголовки берутся у вызывающего, а не
 * подставляются от чужого раздела. Раньше без заголовков запрос уходил с
 * `Referer` и `Origin` сайта R — на сервер раздачи любого раздела.
 */
class PlaylistProbeTest {

    private val helper = M3U8Helper()

    @Test
    fun `без заголовков вызывающего запрос не называет никакой сайт`() {
        val headers = playlistRequestHeaders(null)

        assertNull(headers[HttpHeaders.Referrer])
        assertNull(headers[HttpHeaders.Origin])
        assertTrue("у запроса должен остаться User-Agent", headers.containsKey(HttpHeaders.UserAgent))
    }

    @Test
    fun `заголовки вызывающего уходят как есть`() {
        val own = mapOf(HttpHeaders.Referrer to "https://example.com/", HttpHeaders.UserAgent to "Custom/1.0")

        val headers = playlistRequestHeaders(own)

        assertEquals("https://example.com/", headers[HttpHeaders.Referrer])
        assertEquals("Custom/1.0", headers[HttpHeaders.UserAgent])
        assertEquals(2, headers.size)
    }

    @Test
    fun `свой User-Agent вызывающего в другом регистре не дублируется`() {
        val headers = playlistRequestHeaders(mapOf("user-agent" to "Custom/1.0"))

        assertEquals(mapOf("user-agent" to "Custom/1.0"), headers)
    }

    @Test
    fun `дорожки с региональным тегом языка не отбрасываются`() {
        val m3u8 = """
            #EXTM3U
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="aud",LANGUAGE="pt-BR",NAME="Português",DEFAULT=YES,URI="audio/pt.m3u8"
            #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="sub",LANGUAGE="zh-Hans",NAME="中文",DEFAULT=NO,URI="subs/zh.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            360p.m3u8
        """.trimIndent()

        val data = helper.parseM3U8Content(m3u8, "https://cdn.example.com/hls/master.m3u8")

        assertEquals(listOf("pt-BR"), data.audioTracks.map { it.language })
        assertEquals(listOf("zh-Hans"), data.subtitleTracks.map { it.language })
        assertFalse(data.videoQualities.isEmpty())
    }

    @Test
    fun `эффект поворота не трогают, пока поворот не понадобился`() {
        assertNull(rotationToApply(autoRotate = false, effectsApplied = false))
    }

    @Test
    fun `поворот включается и после выключения сбрасывается в ноль`() {
        assertEquals(-90f, rotationToApply(autoRotate = true, effectsApplied = false))
        assertEquals(-90f, rotationToApply(autoRotate = true, effectsApplied = true))
        assertEquals(0f, rotationToApply(autoRotate = false, effectsApplied = true))
    }
}
