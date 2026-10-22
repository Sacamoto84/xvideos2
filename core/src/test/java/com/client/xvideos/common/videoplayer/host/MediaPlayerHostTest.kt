package com.client.xvideos.common.videoplayer.host

import com.client.xvideos.common.videoplayer.model.MediaClip
import com.client.xvideos.common.videoplayer.model.toPlaybackConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlayerHostTest {

    private fun createHost(
        mediaUrl: String = "http://test.mp4",
        isPaused: Boolean = false,
        isMuted: Boolean = false,
    ): MediaPlayerHost = MediaPlayerHost(
        mediaUrl = mediaUrl,
        isPaused = isPaused,
        isMuted = isMuted,
    )

    // Раньше seekTo писал ещё и seekToTime, и плеер перематывал дважды: по
    // запросу и по seekToTime (проход 210, A1).
    @Test
    fun `seekTo ставит запрос перемотки и время, а seekToTime не трогает`() {
        val host = createHost(mediaUrl = "http://test.mp4")
        host.seekTo(15.5f)

        assertEquals(15.5f, host.seekRequest?.seconds)
        assertNull(host.seekToTime)
        assertEquals(15.5f, host.currentTime, 0.001f)
        assertFalse(host.isSliding)
        host.dispose()
    }

    @Test
    fun `seekTo with negative or NaN seconds ignores invalid input`() {
        val host = createHost(mediaUrl = "http://test.mp4")
        host.seekTo(10f)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(-5f)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(Float.NaN)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)

        host.seekTo(Float.POSITIVE_INFINITY)
        assertNull(host.seekToTime)
        assertEquals(10f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Test
    fun `updateCurrentTime sanitizes negative and NaN values`() {
        val host = createHost(mediaUrl = "http://test.mp4")
        host.updateCurrentTime(42f)
        assertEquals(42f, host.currentTime, 0.001f)

        host.updateCurrentTime(-10f)
        assertEquals(0f, host.currentTime, 0.001f)

        host.updateCurrentTime(30f)
        assertEquals(30f, host.currentTime, 0.001f)

        host.updateCurrentTime(Float.NaN)
        assertEquals(0f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Test
    fun `updateTotalTime sanitizes negative values`() {
        val host = createHost(mediaUrl = "http://test.mp4")
        host.updateTotalTime(120)
        assertEquals(120, host.totalTime)

        host.updateTotalTime(-5)
        assertEquals(0, host.totalTime)
        host.dispose()
    }

    @Test
    fun `play pause and mute controls update state properly`() {
        val host = createHost(mediaUrl = "http://test.mp4", isPaused = true, isMuted = false)
        assertTrue(host.isPaused)
        assertFalse(host.isMuted)

        host.play()
        assertFalse(host.isPaused)

        host.pause()
        assertTrue(host.isPaused)

        host.togglePlayPause()
        assertFalse(host.isPaused)

        host.mute()
        assertTrue(host.isMuted)
        assertEquals(0f, host.volumeLevel, 0.001f)

        host.unmute()
        assertFalse(host.isMuted)
        assertEquals(1f, host.volumeLevel, 0.001f)

        host.toggleMuteUnmute()
        assertTrue(host.isMuted)
        host.dispose()
    }

    @Test
    fun `loadUrl меняет адрес, заголовки и настройку DRM`() {
        val host = createHost(mediaUrl = "https://example.com/first.m3u8")
        val headers = mapOf("Referer" to "https://example.com/")

        host.loadUrl("https://example.com/second.m3u8", headers = headers)

        assertEquals("https://example.com/second.m3u8", host.url)
        assertEquals(headers, host.headers)
        assertNull(host.drmConfig)
        host.dispose()
    }

    // Опрос позиции плеера (раз в 50 мс) раньше сбрасывал цель перемотки, если
    // успевал до перекомпоновки: перемотка при отпускании ползунка терялась, а
    // время откатывалось к старой позиции. Запрос держится, пока плеер его не
    // выполнит.

    @Test
    fun `позиция от плеера до выполнения перемотки не сбрасывает её и не откатывает время`() {
        val host = createHost()
        host.seekTo(30f)
        host.onPlayerPosition(10f)
        assertEquals(30f, host.currentTime)
        assertEquals(30f, host.seekRequest?.seconds)
        host.dispose()
    }

    @Test
    fun `после выполнения перемотки позиция от плеера снова обновляет время и снимает перемотку на ходу`() {
        val host = createHost()
        // Перетаскивание с перемоткой на ходу, затем отпускание.
        host.isSliding = true
        host.seekToTime = 30f
        host.seekTo(30f)
        host.onSeekApplied(host.seekRequest!!.id)
        host.onPlayerPosition(31f)
        assertEquals(31f, host.currentTime)
        assertNull(host.seekToTime)
        assertNull(host.seekRequest)
        host.dispose()
    }

    @Test
    fun `повторная перемотка в ту же точку — новый запрос`() {
        val host = createHost()
        host.seekTo(30f)
        val first = host.seekRequest!!.id
        host.onSeekApplied(first)
        host.seekTo(30f)
        assertTrue(host.seekRequest!!.id != first)
        host.dispose()
    }

    @Test
    fun `подтверждение старого запроса не снимает новый`() {
        val host = createHost()
        host.seekTo(10f)
        val old = host.seekRequest!!.id
        host.seekTo(20f)
        host.onSeekApplied(old)
        assertEquals(20f, host.seekRequest?.seconds)
        host.dispose()
    }

    @Test
    fun `пока ползунок держат, позиция от плеера не меняет время`() {
        val host = createHost()
        host.updateCurrentTime(50f)
        host.isSliding = true
        host.onPlayerPosition(5f)
        assertEquals(50f, host.currentTime)
        host.dispose()
    }

    @Test
    fun `невалидная перемотка запроса не создаёт`() {
        val host = createHost()
        host.seekTo(Float.NaN)
        host.seekTo(-1f)
        assertNull(host.seekRequest)
        host.dispose()
    }

    // Свайп по видео перематывает на шаг от текущего времени (спека
    // 2026-10-21-x-player-swipe-seek-local-fullscreen-design.md).

    @Test
    fun `seekBy сдвигает от текущего времени и складывается до выполнения перемотки`() {
        val host = createHost()
        host.updateTotalTime(100)
        host.updateCurrentTime(10f)

        host.seekBy(5f)
        host.seekBy(5f)

        assertEquals(20f, host.currentTime, 0.001f)
        assertEquals(20f, host.seekRequest?.seconds)
        host.dispose()
    }

    @Test
    fun `seekBy не выходит за начало и конец`() {
        val host = createHost()
        host.updateTotalTime(100)

        host.updateCurrentTime(3f)
        host.seekBy(-5f)
        assertEquals(0f, host.currentTime, 0.001f)

        host.updateCurrentTime(98f)
        host.seekBy(5f)
        assertEquals(100f, host.currentTime, 0.001f)
        host.dispose()
    }

    @Test
    fun `seekBy без длительности ничего не делает`() {
        val host = createHost()
        host.updateCurrentTime(10f)

        host.seekBy(5f)

        assertEquals(10f, host.currentTime, 0.001f)
        assertNull(host.seekRequest)
        host.dispose()
    }

    @Test
    fun `отрезки хоста доходят до плеера`() {
        val clips = listOf(MediaClip(1_000, 2_000), MediaClip(5_000, 6_000))
        val host = MediaPlayerHost(mediaUrl = "file:///v.mp4", clips = clips)

        assertEquals(clips, host.toPlaybackConfig().clips)
        host.dispose()
    }

    // Конец ролика переключал паузу: свайп к самому концу на паузе её снимал
    // (финальное ревью плана свайпа, M2).
    @Test
    fun `конец ролика ставит паузу и не снимает уже стоящую`() {
        val playing = createHost()
        playing.isLooping = false
        playing.onMediaEnded()
        assertTrue(playing.isPaused)

        val paused = createHost(isPaused = true)
        paused.isLooping = false
        paused.onMediaEnded()
        assertTrue(paused.isPaused)
        playing.dispose()
        paused.dispose()
    }

    @Test
    fun `зацикленный ролик в конце на паузу не встаёт`() {
        val host = createHost()
        host.isLooping = true

        host.onMediaEnded()

        assertFalse(host.isPaused)
        host.dispose()
    }

    @Test
    fun `seekBy не трогает паузу`() {
        val host = createHost(isPaused = true)
        host.updateTotalTime(100)

        host.seekBy(5f)

        assertTrue(host.isPaused)
        host.dispose()
    }
}
