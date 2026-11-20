package com.client.xvideos.n

import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.search.NSubmittedSearch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NRepositoryGalleryTest {
    @Test
    fun `an earlier gallery response does not overwrite a forced refresh`() = runTest {
        val older = CompletableDeferred<NNetworkResult>()
        val url = "https://nhentai.net/g/42/"
        val refreshed = NNetworkResult.Success("""{"id":42,"media_id":17,"pages":[{"t":"p"}]}""", url)
        var requests = 0
        val repository = NRepository {
            if (++requests == 1) older.await() else refreshed
        }

        val pending = async(start = CoroutineStart.UNDISPATCHED) { repository.getGalleryDetails(42) }
        val gallery = repository.getGalleryDetails(42, forceRefresh = true).getOrThrow()
        older.complete(NNetworkResult.Success("""{"id":42,"media_id":17,"pages":[{"t":"j"}]}""", url))
        assertEquals("jpg", pending.await().getOrThrow().pages.single().ext)

        assertEquals("png", gallery.pages.single().ext)
        assertSame(gallery, repository.getGalleryDetails(42).getOrThrow())
        assertEquals(2, requests)
    }

    @Test
    fun `detail without pages falls back to readable HTML and only caches that album`() = runTest {
        val calls = mutableListOf<String>()
        val summary = """{"id":42,"media_id":17,"num_pages":12}"""
        val details = """{"id":42,"media_id":17,"pages":[{"path":"galleries/17/1.webp"}]}"""
        val repository = NRepository { path ->
            calls += path
            val body = if (path.startsWith("/api/")) summary else
                """<script type="application/json">$summary</script><script type="application/json">$details</script>"""
            NNetworkResult.Success(body, "https://nhentai.net$path")
        }

        val gallery = repository.getGalleryDetails(42).getOrThrow()

        assertEquals("galleries/17/1.webp", gallery.pages.single().path)
        assertSame(gallery, repository.getGalleryDetails(42).getOrThrow())
        assertEquals(listOf("/api/v2/galleries/42?include=related", "/g/42/"), calls)
    }

    @Test
    fun `detail with no readable version fails and is retried later`() = runTest {
        val calls = mutableListOf<String>()
        val summary = """{"id":42,"media_id":17,"num_pages":12}"""
        val repository = NRepository { path ->
            calls += path
            NNetworkResult.Success(summary, "https://nhentai.net$path")
        }

        repeat(2) { assertTrue(repository.getGalleryDetails(42).isFailure) }
        assertEquals(4, calls.size)
    }

    @Test
    fun `valid empty API pages stay empty without invoking a fallback`() = runTest {
        val calls = mutableListOf<String>()
        val repository = NRepository { path ->
            calls += path
            NNetworkResult.Success("""{"result":[],"num_pages":4}""", "https://nhentai.net$path")
        }

        val feed = repository.getGalleriesPage(page = 3).getOrThrow()
        val tagged = repository.getGalleriesByTagPage(NTag(id = 17, name = "example"), page = 3).getOrThrow()

        assertTrue(feed.items.isEmpty())
        assertEquals(4, feed.totalPages)
        assertEquals(feed, tagged)
        assertEquals(
            listOf("/api/v2/galleries?page=3&per_page=25", "/api/v2/galleries/tagged?tag_id=17&page=3&sort=date"),
            calls
        )
    }

    @Test
    fun `tag fallback uses an escaped and typed tag expression`() = runTest {
        val calls = mutableListOf<String>()
        val repository = NRepository { path ->
            calls += path
            if (path.startsWith("/api/v2/galleries/tagged")) {
                NNetworkResult.Error(404, "Not found")
            } else {
                NNetworkResult.Success("""{"result":[],"num_pages":0}""", "https://nhentai.net$path")
            }
        }

        repository.getGalleriesByTagPage(NTag(id = 17, type = "tag", name = "a \"quote\" \\ test")).getOrThrow()

        val query = ("https://nhentai.net" + calls.last()).toHttpUrl().queryParameter("query")
        assertEquals("""tag:"a \"quote\" \\ test"""", query)
    }

    @Test
    fun `search with only a sort loads the corresponding catalog page`() = runTest {
        val calls = mutableListOf<String>()
        val repository = NRepository { path ->
            calls += path
            NNetworkResult.Success("""{"result":[],"num_pages":4}""", "https://nhentai.net$path")
        }
        val search = NSubmittedSearch("", NSortType.POPULAR_WEEK)

        assertTrue(search.isActive)
        assertEquals(2, search.loadPage(repository, 2).getOrThrow().page)
        assertEquals(listOf("/api/v2/search?query=-nonexisttag&sort=popular-week&page=2"), calls)
    }
}
