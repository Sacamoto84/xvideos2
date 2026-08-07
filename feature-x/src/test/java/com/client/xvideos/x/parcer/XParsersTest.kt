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

    // --- parserScreenTags: число страниц -------------------------------------

    /**
     * Разметка сокращена с живой страницы `/tags/public`: та же вложенность и
     * те же классы, выброшены только промежуточные номера.
     */
    private fun paginationHtml(body: String) = """
        <html><body>
          <h2 class="page-title">public<span class="sub">Видео с тегом « public » (17 417 результаты)</span></h2>
          <div class="pagination "><ul>$body</ul></div>
          <div id="content">
            <div class="mozaique cust-nb-cols"></div>
          </div>
        </body></html>
    """.trimIndent()

    /** Последняя страница помечена классом — берём её метку, а не число ссылок. */
    @Test
    fun `число страниц читается из last-page`() {
        val html = paginationHtml(
            """
            <li><a class="active" href="">1</a></li>
            <li><a href="/tags/public/1">2</a></li>
            <li class="no-page"><a href="#" class="ellipsis last-ellipsis">...</a>
            <li><a href="/tags/public/148" class="last-page">149</a></li>
            <li><a href="/tags/public/1" class="no-page next-page"><span>Следующий</span></a></li>
            """.trimIndent()
        )

        assertEquals(149, parserScreenTags(html).lastPage)
    }

    /** Страниц мало — список умещается целиком, last-page сайт не ставит. */
    @Test
    fun `без last-page берётся наибольшая метка`() {
        val html = paginationHtml(
            """
            <li><a class="active" href="">1</a></li>
            <li><a href="/tags/rare/1">2</a></li>
            <li><a href="/tags/rare/2">3</a></li>
            <li><a href="/tags/rare/1" class="no-page next-page"><span>Следующий</span></a></li>
            """.trimIndent()
        )

        assertEquals(3, parserScreenTags(html).lastPage)
    }

    /** Блока постраничности нет вовсе — страница одна, а не ноль. */
    @Test
    fun `без блока постраничности страница одна`() {
        val html = """
            <html><body>
              <h2 class="page-title">rare<span class="sub">Видео с тегом « rare »</span></h2>
              <div id="content"><div class="mozaique cust-nb-cols"></div></div>
            </body></html>
        """.trimIndent()

        assertEquals(1, parserScreenTags(html).lastPage)
    }

    /** Заголовок и карточки разбираются по-прежнему. */
    @Test
    fun `заголовок и карточки страницы тега разбираются`() {
        val html = paginationHtml(
            """<li><a href="/tags/public/148" class="last-page">149</a></li>"""
        ).replace(
            """<div class="mozaique cust-nb-cols"></div>""",
            """
            <div class="mozaique cust-nb-cols">
              <div data-id="70057387" class="frame-block thumb-block  ">
                <p class="title"><a href="/video.uicfdab07bd/_" title="Название">Название</a>
                  <span class="duration">10 мин.</span></p>
                <p class="metadata"><a href="/channels/some" class="name">Some</a></p>
                <img data-src="https://thumbs-gcore.xvideos-cdn.com/abc/0/xv_18_t.jpg">
              </div>
            </div>
            """.trimIndent()
        )

        val screen = parserScreenTags(html)

        assertEquals("public", screen.title0)
        assertEquals(1, screen.items.size)
        assertEquals(70057387L, screen.items[0].id)
        assertEquals("/video.uicfdab07bd/_", screen.items[0].href)
    }
}
