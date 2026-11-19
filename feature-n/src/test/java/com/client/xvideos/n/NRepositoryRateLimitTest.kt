package com.client.xvideos.n

import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.repository.NRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NRepositoryRateLimitTest {
    @Test
    fun `HTTP 429 skips every gallery fallback`() = runTest {
        val calls = mutableListOf<String>()
        val rateLimit = NNetworkResult.Error(429, "Повторите через 10 сек.")
        val repository = NRepository { url ->
            calls += url
            rateLimit
        }

        val results = listOf(
            repository.getGalleriesPage(),
            repository.getGalleriesByTagPage(NTag(id = 17, name = "test")),
            repository.getGalleryDetails(42),
            repository.searchPage("test")
        )

        assertTrue(results.all { it.exceptionOrNull()?.message == rateLimit.message })
        assertEquals(
            listOf(
                "/api/v2/galleries?page=1&per_page=25",
                "/api/v2/galleries/tagged?tag_id=17&page=1&sort=date",
                "/api/v2/galleries/42?include=related",
                "/api/v2/search?query=test&page=1&sort=date"
            ),
            calls
        )
    }
}
