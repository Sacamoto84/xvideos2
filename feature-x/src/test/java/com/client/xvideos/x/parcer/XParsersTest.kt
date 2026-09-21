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

    @Test
    fun `протокольно-относительные адреса нормализуются в https`() {
        val script = "html5player.setVideoHLS('\\/\\/cdn.example.com\\/master.m3u8');"

        val config = parseHTML5Player(script)

        assertNotNull(config)
        assertEquals("https://cdn.example.com/master.m3u8", config!!.videoHLS)
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

    /** Если в пагинации стоит 0 или отрицательное число — число страниц остаётся не менее 1 (UI65). */
    @Test
    fun `при нулевом или некорректном last-page число страниц не падает ниже 1`() {
        val html0 = paginationHtml("""<li><a href="/tags/public/0" class="last-page">0</a></li>""")
        assertEquals(1, parserScreenTags(html0).lastPage)

        val htmlNeg = paginationHtml("""<li><a href="/tags/public/-1" class="last-page">-5</a></li>""")
        assertEquals(1, parserScreenTags(htmlNeg).lastPage)
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

    // --- parserItemVideoTags -------------------------------------------------

    @Test
    fun `теги очищаются от пробелов и дедуплицируются`() {
        val html = """
            <html><body>
              <ul>
                <li><a class="is-keyword" href="/tags/blonde"> blonde </a></li>
                <li><a class="is-keyword" href="/tags/blonde">blonde</a></li>
                <li><a class="is-keyword" href="/tags/teen">teen</a></li>
                <li><a class="is-keyword" href="/tags/empty">   </a></li>
              </ul>
              <li class="main-uploader">
                <a href="/channels/sweet">
                  <span class="name"><span>icon</span> Sweet Channel </span>
                  <span class="count">10k</span>
                </a>
              </li>
              <li class="model">
                <a href="/pornstars/star">
                  <span class="name">Star Name</span>
                  <span class="count">5k</span>
                </a>
              </li>
            </body></html>
        """.trimIndent()

        val result = parserItemVideoTags(html)

        assertEquals(listOf("blonde", "teen"), result.tags)
        assertEquals(1, result.mainUploader.size)
        assertEquals("Sweet Channel", result.mainUploader[0].name)
        assertEquals(1, result.pornstars.size)
        assertEquals("Star Name", result.pornstars[0].name)
    }

    @Test
    fun `однократный разбор Document извлекает плеер и теги без повторного парсинга`() {
        val html = """
            <html><body>
              <div id="video-player-bg">
                <script>html5player.setVideoHLS('https://cdn/hls.m3u8');</script>
              </div>
              <ul>
                <li><a class="is-keyword" href="/tags/hd">HD</a></li>
              </ul>
            </body></html>
        """.trimIndent()

        val doc = org.jsoup.Jsoup.parse(html)
        val script = parserItemVideo(doc)
        val tags = parserItemVideoTags(doc)

        assertNotNull(script)
        assertTrue(script!!.contains("setVideoHLS"))
        assertEquals(listOf("HD"), tags.tags)
    }

    // --- parserVideoPreviewFromImageUrl -------------------------------------

    @Test
    fun `legacy cdn url разбирается в видеопревью`() {
        val imgUrl = "https://cdn77-pic.xvideos-cdn.com/videos/thumbs169ll/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad/6a4f6bafe3abb03b5ea6108ab18ff1ad.30.jpg"
        val preview = parserVideoPreviewFromImageUrl(imgUrl)
        assertEquals(
            "https://cdn77-pic.xvideos-cdn.com/videos/videopreview/6a/4f/6b/6a4f6bafe3abb03b5ea6108ab18ff1ad_169.mp4",
            preview
        )
    }

    @Test
    fun `new cdn url разбирается в preview_mp4`() {
        val imgUrl = "https://thumbs-gcore.xvideos-cdn.com/abc/0/xv_18_t.jpg"
        val preview = parserVideoPreviewFromImageUrl(imgUrl)
        assertEquals(
            "https://thumbs-gcore.xvideos-cdn.com/abc/0/preview.mp4",
            preview
        )
    }

    @Test
    fun `некорректные адреса и null возвращают null`() {
        assertNull(parserVideoPreviewFromImageUrl(null))
        assertNull(parserVideoPreviewFromImageUrl(""))
        assertNull(parserVideoPreviewFromImageUrl("   "))
        assertNull(parserVideoPreviewFromImageUrl("null"))
        assertNull(parserVideoPreviewFromImageUrl("https://cdn.example.com/other/path/image.jpg"))
    }

    // --- parserListVideo & parseSiteCountryFlag ------------------------------

    @Test
    fun `разбор списка видео извлекает валидные блоки и отсекает некорректные`() {
        val html = """
            <div class="mozaique">
              <div data-id="123456" class="frame-block">
                <p class="title"><a href="/video123456/test_title" title="Test Title">Test Title</a>
                  <span class="duration">12 min</span>
                </p>
                <p class="metadata"><a href="/channels/mychannel" class="name">MyChannel</a> 500k Views</p>
                <img data-src="https://thumbs-gcore.xvideos-cdn.com/abc/0/xv_18_t.jpg">
              </div>
              <div class="frame-block">
                <p class="title"><a href="/video999/bad">Bad missing id</a></p>
              </div>
              <div data-id="-10" class="frame-block">
                <p class="title"><a href="/video-10/bad">Bad negative id</a></p>
              </div>
              <div data-id="9999" class="frame-block">
                <p class="title"><a href="">No Link</a></p>
              </div>
            </div>
        """.trimIndent()

        val doc = org.jsoup.Jsoup.parse(html)
        val list = parserListVideo(doc)

        assertEquals(1, list.size)
        val item = list[0]
        assertEquals(123456L, item.id)
        assertEquals("Test Title", item.title)
        assertEquals("/video123456/test_title", item.href)
        assertEquals("12 min", item.duration)
        assertEquals("500k", item.views)
        assertEquals("MyChannel", item.channel)
        assertEquals("https://thumbs-gcore.xvideos-cdn.com/abc/0/xv_18_t.jpg", item.previewImage)
        assertEquals("https://thumbs-gcore.xvideos-cdn.com/abc/0/preview.mp4", item.previewVideo)
        assertEquals("MyChannel", item.nameProfile)
        assertEquals("/channels/mychannel", item.linkProfile)
    }

    @Test
    fun `флаг локализации извлекается из блока site-localisation`() {
        val htmlWithFlag = """
            <div id="site-localisation">
              <span class="flag-de">Deutschland</span>
            </div>
        """.trimIndent()

        val flag = parseSiteCountryFlag(htmlWithFlag)
        assertNotNull(flag)
        assertEquals(com.client.xvideos.x.model.getFlagEmoji("flag-de"), flag)

        val htmlWithoutFlag = """<div><span>No localisation</span></div>"""
        assertNull(parseSiteCountryFlag(htmlWithoutFlag))
    }

    @Test
    fun `пустые и пробельные строки безопасно обрабатываются парсерами`() {
        assertNull(parserItemVideo(""))
        assertNull(parserItemVideo("   "))

        assertNull(parseHTML5Player(""))
        assertNull(parseHTML5Player("   \n\t  "))

        assertNull(parseSiteCountryFlag(""))
        assertNull(parseSiteCountryFlag("   "))

        assertTrue(parserListVideo("").isEmpty())
        assertTrue(parserListVideo("   ").isEmpty())

        val defaultScreenTags = parserScreenTags("")
        assertEquals("?", defaultScreenTags.title0)
        assertEquals("?", defaultScreenTags.title1)
        assertEquals(1, defaultScreenTags.lastPage)
        assertTrue(defaultScreenTags.items.isEmpty())

        val blankScreenTags = parserScreenTags("   \n  ")
        assertEquals("?", blankScreenTags.title0)
        assertEquals("?", blankScreenTags.title1)
        assertEquals(1, blankScreenTags.lastPage)
        assertTrue(blankScreenTags.items.isEmpty())
    }

    @Test
    fun `parserScreenTags extracts correct video attributes and positive IDs`() {
        val html = """
            <h2 class="page-title">Tag Title <span class="sub">123 videos</span></h2>
            <div class="pagination">
                <a href="/tags/test/1">1</a>
                <a href="/tags/test/2">2</a>
                <a href="/tags/test/5" class="last-page">5</a>
            </div>
            <div id="content">
              <div class="mozaique cust-nb-cols">
                <div class="frame-block thumb-block" data-id="555123">
                  <p class="title"><a href="/video555123/cool_video" title="Cool Video"><span class="duration">15 min</span></a></p>
                  <p class="metadata"><span class="name">StarChannel</span><span class="bg"><span><span>1.2M</span></span></span><a href="/profiles/star"></a></p>
                  <img data-src="https://img.xv/poster.jpg" />
                </div>
                <div class="frame-block thumb-block">
                  <p class="title"><a href="/video777999/fallback_video" title="Fallback Video"><span class="duration">10 min</span></a></p>
                  <p class="metadata"><span class="name">OtherChannel</span></p>
                </div>
                <div class="frame-block thumb-block">
                  <p class="title"><a href="Нет ссылки" title="No Link Video"></a></p>
                </div>
              </div>
            </div>
        """.trimIndent()

        val result = parserScreenTags(html)
        assertEquals("Tag Title", result.title0)
        assertEquals("123 videos", result.title1)
        assertEquals(5, result.lastPage)
        assertEquals(2, result.items.size)

        val first = result.items[0]
        assertEquals(555123L, first.id)
        assertEquals("Cool Video", first.title)
        assertEquals("/video555123/cool_video", first.href)
        assertEquals("15 min", first.duration)
        assertEquals("StarChannel", first.channel)
        assertEquals("1.2M", first.views)
        assertEquals("https://img.xv/poster.jpg", first.previewImage)

        val second = result.items[1]
        assertEquals(777999L, second.id)
        assertTrue(second.id > 0L)
        assertEquals("Fallback Video", second.title)
    }

    @Test
    fun `parserScreenTags skips malformed cards without dropping valid cards`() {
        val html = """
            <div id="content">
              <div class="mozaique cust-nb-cols">
                <div class="frame-block thumb-block">
                  <!-- Broken card with no link element -->
                </div>
                <div class="frame-block thumb-block" data-id="101">
                  <p class="title"><a href="/video101/valid" title="Valid"></a></p>
                </div>
              </div>
            </div>
        """.trimIndent()

        val result = parserScreenTags(html)
        assertEquals(1, result.items.size)
        assertEquals(101L, result.items[0].id)
    }
}
