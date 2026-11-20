package com.client.xvideos.n.ui.screens.explorer.tab.list

import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.repository.CloudflareBlockedException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NGalleryPagesTest {
    @Test
    fun `a blocked preloaded page is exposed only when it becomes visible`() = runTest {
        val challenge = CloudflareBlockedException("https://nhentai.net/")
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            if (number == 2) Result.failure(challenge) else Result.success(NGalleryPageResult(totalPages = 2))
        }
        pages.load(0)
        runCurrent()
        pages.load(1)
        runCurrent()

        assertTrue(pages[1] is NGalleryListUiState.CloudflareRequired)
        assertNull(pages.visibleChallenge)
        pages.position.scrollToPage(1)
        assertSame(challenge, pages.visibleChallenge)
    }

    @Test
    fun `page numbers are one based and revisiting a page reuses its data and position`() = runTest {
        val requests = mutableListOf<Int>()
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            requests += number
            Result.success(NGalleryPageResult(listOf(NGalleryPreview(id = number.toLong())), number, 3))
        }
        pages.load(0)
        pages.load(0)
        runCurrent()
        pages.load(1)
        pages.load(2)
        runCurrent()
        val grid = pages.grid(1)
        pages.load(0)
        pages.load(1)
        pages.load(3)
        runCurrent()

        assertEquals(listOf(1, 2, 3), requests)
        assertSame(grid, pages.grid(1))
        assertEquals(2L, (pages[1] as NGalleryListUiState.Success).items.single().id)
    }

    @Test
    fun `reset discards old requests and positions`() = runTest {
        val old = CompletableDeferred<NGalleryPageResult>()
        var waiting = true
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) {
            Result.success(if (waiting) old.await() else NGalleryPageResult(totalPages = 4))
        }
        pages.load(0)
        runCurrent()
        val grid = pages.grid(0)
        val pager = pages.position
        pages.reset()
        waiting = false
        pages.load(0)
        old.complete(NGalleryPageResult(totalPages = 20))
        runCurrent()

        assertEquals(4, pages.totalPages)
        assertNotSame(grid, pages.grid(0))
        assertNotSame(pager, pages.position)
        assertEquals(0, pages.currentIndex)
    }

    @Test
    fun `failure and retry affect only their page and refresh retains the visible index`() = runTest {
        var fail = true
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            if (number == 2 && fail) Result.failure(IllegalStateException("ошибка"))
            else Result.success(NGalleryPageResult(page = number, totalPages = 3))
        }
        pages.load(0)
        runCurrent()
        val first = pages[0]
        pages.load(1)
        runCurrent()
        assertTrue(pages[1] is NGalleryListUiState.Error)
        fail = false
        pages.retry(1)
        runCurrent()
        assertSame(first, pages[0])
        assertTrue(pages[1] is NGalleryListUiState.Success)
        pages.position.scrollToPage(1)
        pages.refresh()
        assertEquals(1, pages.currentIndex)
        assertNull(pages[0])
        assertNull(pages[1])
    }

    @Test
    fun `a smaller list moves the pager and discards removed pages and their positions`() = runTest {
        var count = 3
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            Result.success(NGalleryPageResult(page = number, totalPages = count))
        }
        pages.load(0)
        runCurrent()
        pages.position.scrollToPage(2)
        pages.load(2)
        runCurrent()
        val removedGrid = pages.grid(2)

        count = 2
        pages.retry(0)
        runCurrent()

        assertEquals(2, pages.totalPages)
        assertEquals(1, pages.currentIndex)
        assertNull(pages[2])
        assertNotSame(removedGrid, pages.grid(2))
    }

    @Test
    fun `an older prefetch cannot revive a removed page`() = runTest {
        val prefetch = CompletableDeferred<NGalleryPageResult>()
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            val result = when (number) {
                1 -> NGalleryPageResult(page = 1, totalPages = 3)
                2 -> NGalleryPageResult(page = 2, totalPages = 2)
                else -> prefetch.await()
            }
            Result.success(result)
        }
        pages.load(0)
        runCurrent()
        pages.load(2)
        runCurrent()
        pages.load(1)
        runCurrent()
        prefetch.complete(NGalleryPageResult(page = 3, totalPages = 3))
        runCurrent()

        assertEquals(2, pages.totalPages)
        assertNull(pages[2])
    }

    @Test
    fun `a slow response for a valid page cannot overwrite a newer page count`() = runTest {
        val olderPage = CompletableDeferred<NGalleryPageResult>()
        var refreshing = false
        val pages = NGalleryPages(backgroundScope, StandardTestDispatcher(testScheduler)) { number ->
            Result.success(
                when {
                    number == 2 -> NGalleryPageResult(page = 2, totalPages = 2)
                    refreshing -> olderPage.await()
                    else -> NGalleryPageResult(page = 1, totalPages = 3)
                }
            )
        }
        pages.load(0)
        runCurrent()
        refreshing = true
        pages.retry(0)
        runCurrent()
        pages.load(1)
        runCurrent()
        olderPage.complete(NGalleryPageResult(listOf(NGalleryPreview(id = 1L)), page = 1, totalPages = 3))
        runCurrent()

        assertEquals(2, pages.totalPages)
        assertEquals(1L, (pages[0] as NGalleryListUiState.Success).items.single().id)
    }
}
