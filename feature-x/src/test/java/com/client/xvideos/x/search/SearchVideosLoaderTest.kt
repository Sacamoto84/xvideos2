package com.client.xvideos.x.search

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVideosLoaderTest {

    @Test
    fun `encodeSearchVideosQuery правильно кодирует строки`() {
        assertEquals("", encodeSearchVideosQuery(""))
        assertEquals("", encodeSearchVideosQuery("   "))
        assertEquals("lap+dance", encodeSearchVideosQuery("lap dance"))
        assertTrue(encodeSearchVideosQuery("танец").isNotEmpty())
    }

    @Test
    fun `buildSearchVideosUrl правильно формирует URL с учетом пагинации`() {
        assertEquals("https://www.xv-ru.com/?k=dance", buildSearchVideosUrl("dance", 0))
        assertEquals("https://www.xv-ru.com/?k=dance", buildSearchVideosUrl("dance", -1))
        assertEquals("https://www.xv-ru.com/?k=dance&p=1", buildSearchVideosUrl("dance", 1))
        assertEquals("https://www.xv-ru.com/?k=dance&p=14", buildSearchVideosUrl("dance", 14))
    }

    @Test
    fun `parseSearchLastPage извлекает номер последней страницы`() {
        val htmlWithLastPage = """
            <div class="pagination">
                <ul>
                    <li><a class="active" href="">1</a></li>
                    <li><a href="/?k=dance&p=1">2</a></li>
                    <li class="no-page"><a href="#" class="ellipsis">...</a></li>
                    <li><a href="/?k=dance&p=148" class="last-page">149</a></li>
                    <li><a href="/?k=dance&p=1" class="next-page">Следующий</a></li>
                </ul>
            </div>
        """.trimIndent()

        val doc1 = Jsoup.parse(htmlWithLastPage)
        assertEquals(149, parseSearchLastPage(doc1))

        val htmlWithoutLastPageClass = """
            <div class="pagination">
                <ul>
                    <li><a class="active" href="">1</a></li>
                    <li><a href="/?k=dance&p=1">2</a></li>
                    <li><a href="/?k=dance&p=2">3</a></li>
                </ul>
            </div>
        """.trimIndent()

        val doc2 = Jsoup.parse(htmlWithoutLastPageClass)
        assertEquals(3, parseSearchLastPage(doc2))

        val emptyDoc = Jsoup.parse("<div>no pagination</div>")
        assertEquals(1, parseSearchLastPage(emptyDoc))
    }

    @Test
    fun `parseSearchVideosPage парсит карточки видео и страницы`() {
        val html = """
            <div class="pagination">
                <a class="last-page">25</a>
            </div>
            <div class="mozaique cust-nb-cols">
                <div id="video_1" data-id="1001" class="frame-block thumb-block">
                    <div class="thumb-inside">
                        <div class="thumb">
                            <a href="/video1001/best_video">
                                <img data-src="https://img.xv-ru.com/1001.jpg" />
                            </a>
                        </div>
                    </div>
                    <div class="thumb-under">
                        <p class="title"><a href="/video1001/best_video" title="Best Dance Video">Best Dance Video <span class="duration">10 min</span></a></p>
                        <p class="metadata"><span class="bg"><span class="name">Studio1</span></span></p>
                    </div>
                </div>
                <div id="video_2" data-id="1002" class="frame-block thumb-block">
                    <div class="thumb-inside">
                        <div class="thumb">
                            <a href="/video1002/great_dance">
                                <img data-src="https://img.xv-ru.com/1002.jpg" />
                            </a>
                        </div>
                    </div>
                    <div class="thumb-under">
                        <p class="title"><a href="/video1002/great_dance" title="Great Dance">Great Dance <span class="duration">5 min</span></a></p>
                        <p class="metadata"><span class="bg"><span class="name">Channel2</span></span></p>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val result = parseSearchVideosPage(html)
        assertEquals(25, result.maxPages)
        assertEquals(2, result.items.size)
        assertEquals(1001L, result.items[0].id)
        assertEquals("Best Dance Video 10 min", result.items[0].title)
        assertEquals(1002L, result.items[1].id)
        assertEquals("Great Dance 5 min", result.items[1].title)
    }

    @Test
    fun `parseSearchVideosPage безопасно обрабатывает пустой HTML`() {
        val result = parseSearchVideosPage("")
        assertEquals(1, result.maxPages)
        assertEquals(0, result.items.size)
    }
}
