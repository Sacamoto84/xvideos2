package com.client.xvideos.x.parcer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Разбор страниц X.
 *
 * Это единственный слой проекта, который ломается от чужих правок — на стороне
 * сайта, без предупреждения и без единой ошибки в логе. До этих тестов он не был
 * покрыт ничем: во всём `:feature-x` лежал один `ItemsXGsonTest`.
 *
 * Разметка в фикстурах намеренно сокращена до того, за что цепляется разбор.
 */
class XParsersTest {

    // --- parserItemVideo -----------------------------------------------------

    /**
     * Скрипт ищется по содержимому, а не по месту. Раньше стоял селектор
     * `#video-player-bg > script:nth-child(6)`: один лишний тег выше — и разбор
     * молча получал пустую строку.
     */
    @Test
    fun `скрипт плеера находится не на шестом месте`() {
        val html = """
            <div id="video-player-bg">
              <script>var unrelated = 1;</script>
              <script>html5player.setVideoUrlHigh('https://cdn/high.mp4');</script>
            </div>
        """.trimIndent()

        val script = parserItemVideo(html)

        assertNotNull(script)
        assertTrue(script!!.contains("setVideoUrlHigh"))
    }

    /** Контейнер переименовали — скрипт всё равно должен найтись по странице. */
    @Test
    fun `скрипт плеера находится вне ожидаемого контейнера`() {
        val html = """
            <div id="some-new-wrapper">
              <script>html5player.setVideoUrlLow('https://cdn/low.mp4');</script>
            </div>
        """.trimIndent()

        assertNotNull(parserItemVideo(html))
    }

    @Test
    fun `без скрипта плеера возвращается null`() {
        val html = """
            <div id="video-player-bg">
              <script>var unrelated = 1;</script>
            </div>
        """.trimIndent()

        assertNull(parserItemVideo(html))
    }

    // --- parseHTML5Player ----------------------------------------------------

    @Test
    fun `адреса раскодируются из JS-экранирования`() {
        val script = """
            html5player.setVideoTitle('Заголовок');
            html5player.setVideoUrlHigh('https:\/\/cdn\/high.mp4');
            html5player.setVideoUrlLow('https:\/\/cdn\/low.mp4');
        """.trimIndent()

        val config = parseHTML5Player(script)

        assertNotNull(config)
        assertEquals("https://cdn/high.mp4", config!!.videoUrlHigh)
        assertEquals("https://cdn/low.mp4", config.videoUrlLow)
        assertEquals("Заголовок", config.videoTitle)
    }

    /** Достаточно одного источника: HLS без прогрессивных ссылок — рабочий случай. */
    @Test
    fun `одного HLS хватает`() {
        val script = "html5player.setVideoHLS('https:\\/\\/cdn\\/master.m3u8');"

        val config = parseHTML5Player(script)

        assertNotNull(config)
        assertEquals("https://cdn/master.m3u8", config!!.videoHLS)
    }

    /**
     * Ни одного источника — `null`, а не конфиг с пустыми полями. Раньше отказ
     * выглядел как успех, и плеер молча получал пустые адреса.
     */
    @Test
    fun `без единого источника возвращается null`() {
        val script = "html5player.setVideoTitle('Есть только заголовок');"

        assertNull(parseHTML5Player(script))
    }

    @Test
    fun `пустой скрипт не даёт конфига`() {
        assertNull(parseHTML5Player(""))
    }

    // --- parserVideoPreviewFromImageUrl --------------------------------------

    @Test
    fun `превью собирается из адреса картинки`() {
        val image = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/" +
            "6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"

        assertEquals(
            "https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/" +
                "6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4",
            parserVideoPreviewFromImageUrl(image)
        )
    }

    @Test
    fun `у нового CDN превью лежит рядом`() {
        val image = "https://thumb-cdn77.xvideos-cdn.com/videos/xyz/abc/mozaique.jpg"

        assertEquals(
            "https://thumb-cdn77.xvideos-cdn.com/videos/xyz/abc/preview.mp4",
            parserVideoPreviewFromImageUrl(image)
        )
    }

    /**
     * Признак неудачи — `null`, а не строка `"null"`. Строку проверял один
     * вызывающий из трёх, и она успевала лечь в `ItemsX.previewVideo` и вернуться
     * сюда же на следующем экране.
     */
    @Test
    fun `неудача обозначается null`() {
        assertNull(parserVideoPreviewFromImageUrl(null))
        assertNull(parserVideoPreviewFromImageUrl(""))
        assertNull(parserVideoPreviewFromImageUrl("   "))
        assertNull(parserVideoPreviewFromImageUrl("не адрес вовсе"))
    }

    /** Строку "null" записали в файлы избранного прошлые версии — узнаём её на входе. */
    @Test
    fun `строка null с диска не принимается за адрес`() {
        assertNull(parserVideoPreviewFromImageUrl("null"))
        assertNull(parserVideoPreviewFromImageUrl("NULL"))
    }

    @Test
    fun `параметры запроса и якорь отбрасываются`() {
        val image = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/" +
            "6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg?v=2#frag"

        assertEquals(
            "https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/" +
                "6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4",
            parserVideoPreviewFromImageUrl(image)
        )
    }
}
