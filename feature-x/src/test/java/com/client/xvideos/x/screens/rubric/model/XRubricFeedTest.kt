package com.client.xvideos.x.screens.rubric.model

import androidx.compose.runtime.mutableIntStateOf
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Выдача категории: адрес по фильтрам, кеш страниц и сброс при смене фильтров. */
class XRubricFeedTest {

    /** Разметка страницы категории, сокращённая до того, что читает parserScreenTags. */
    private val page = """
        <html><body>
          <h2 class="page-title">Категория Б<span class="sub">(1 000 результатов)</span></h2>
          <div class="pagination "><ul><li><a href="/c/Cat_B-12/4" class="last-page">5</a></li></ul></div>
          <div id="content"><div class="mozaique cust-nb-cols">
            <div class="frame-block thumb-block" data-id="555123">
              <p class="title"><a href="/video555123/clip" title="Ролик"><span class="duration">15 min</span></a></p>
            </div>
          </div></div>
        </body></html>
    """.trimIndent()

    private val emptyPage = page.replace(
        Regex("""<div class="frame-block.*?</div>\s*""", RegexOption.DOT_MATCHES_ALL),
        "",
    )

    @Test
    fun `страница грузится по адресу текущих фильтров`() {
        val urls = mutableListOf<String>()
        val feed = XRubricFeed(12, "Cat_B") { url -> urls += url; page }

        runBlocking { feed.loadPage(0) }

        assertEquals(listOf("$urlStart/c/Cat_B-12/0"), urls)
        assertEquals(5, feed.screen.lastPage)
        assertEquals("Категория Б", feed.screen.title0)
    }

    @Test
    fun `загруженная страница берётся из кеша`() {
        var calls = 0
        val feed = XRubricFeed(12, "Cat_B") { calls++; page }

        runBlocking {
            feed.loadPage(1)
            feed.loadPage(1)
        }

        assertEquals(1, calls)
        assertEquals(1, feed.cachedItems(1)?.size)
    }

    @Test
    fun `смена фильтров сбрасывает выдачу`() {
        val urls = mutableListOf<String>()
        val feed = XRubricFeed(12, "Cat_B") { url -> urls += url; page }
        runBlocking {
            feed.loadPage(0)
            feed.loadPage(2)
        }
        feed.listPositions[2] = LazyListPosition(3)
        feed.shownPerRow[2] = mutableIntStateOf(2)
        feed.currentPage = 2

        assertTrue(feed.applyFilters(XRubricFilters(sort = XRubricSort.RATING)))

        assertTrue(feed.listPositions.isEmpty())
        assertTrue(feed.shownPerRow.isEmpty())
        assertEquals(0, feed.currentPage)
        assertEquals("", feed.screen.title0)
        runBlocking { feed.loadPage(2) }
        assertEquals("$urlStart/c/s:rating/Cat_B-12/2", urls.last())
        assertEquals(3, urls.size)
    }

    @Test
    fun `те же фильтры — ничего не сбрасывается`() {
        var calls = 0
        val feed = XRubricFeed(12, "Cat_B") { calls++; page }
        runBlocking { feed.loadPage(0) }

        assertFalse(feed.applyFilters(XRubricFilters()))

        runBlocking { feed.loadPage(0) }
        assertEquals(1, calls)
        assertEquals("Категория Б", feed.screen.title0)
    }

    @Test
    fun `ответ на старые фильтры не попадает в новую выдачу`() {
        val staleAnswer = CompletableDeferred<String>()
        var calls = 0
        val feed = XRubricFeed(12, "Cat_B") { calls++; if (calls == 1) staleAnswer.await() else page }

        runBlocking {
            val stale = launch { feed.loadPage(0) }
            yield()
            feed.applyFilters(XRubricFilters(sort = XRubricSort.RATING))
            staleAnswer.complete(page)
            stale.join()

            assertEquals("", feed.screen.title0)
            feed.loadPage(0)
        }

        assertEquals(2, calls)
    }

    @Test
    fun `страница без роликов не кешируется`() {
        var calls = 0
        val feed = XRubricFeed(12, "Cat_B") { calls++; emptyPage }

        runBlocking {
            feed.loadPage(0)
            feed.loadPage(0)
        }

        assertEquals(2, calls)
    }

    @Test
    fun `пустой ответ — IOException`() {
        val feed = XRubricFeed(12, "Cat_B") { "" }

        assertThrows(IOException::class.java) { runBlocking { feed.loadPage(0) } }
    }
}
