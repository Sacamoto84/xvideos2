package com.client.xvideos.l.net.graphQl

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class LandingPageAlbumHandlerTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_landing").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    private class FakeRepository(
        private val responseResult: Result<String> = Result.success("{}")
    ) : Repository(AppFileDatabase()) {
        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> = responseResult
    }

    @Test
    fun `LandingPageAlbumTag returns failure on blank tag`() = runBlocking {
        val repo = FakeRepository()
        val result = LandingPageAlbumTag(tag = "   ", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Tag cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumTag returns failure on repository network error`() = runBlocking {
        val repo = FakeRepository(Result.failure(IOException("Network unreachable")))
        val result = LandingPageAlbumTag(tag = "123", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Network unreachable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumTag returns failure with GraphQL error message`() = runBlocking {
        val errorJson = """
            {
                "errors": [
                    {"message": "Tag not found or private", "code": 404}
                ]
            }
        """.trimIndent()
        val repo = FakeRepository(Result.success(errorJson))
        val result = LandingPageAlbumTag(tag = "private_tag", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Tag not found or private", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumTag parses valid landing page data`() = runBlocking {
        val validJson = """
            {
                "data": {
                    "landing_page_album": {
                        "tag": {
                            "title": "Cosplay Specials",
                            "sections": [
                                {
                                    "title": "Top Hits",
                                    "count": 5,
                                    "item_type": "album",
                                    "url": "https://example.com",
                                    "items": []
                                }
                            ]
                        }
                    }
                }
            }
        """.trimIndent()
        val repo = FakeRepository(Result.success(validJson))
        val result = LandingPageAlbumTag(tag = "cosplay", repository = repo)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("Cosplay Specials", data.title)
        assertEquals(1, data.sections.size)
        assertEquals("Top Hits", data.sections[0].title)
    }

    @Test
    fun `LandingPageAlbumSearch returns failure on blank query`() = runBlocking {
        val repo = FakeRepository()
        val result = LandingPageAlbumSearch(search = "", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Search query cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumSearch returns failure on repository network error`() = runBlocking {
        val repo = FakeRepository(Result.failure(IOException("Connection timed out")))
        val result = LandingPageAlbumSearch(search = "anime", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Connection timed out", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumSearch returns failure with GraphQL error message`() = runBlocking {
        val errorJson = """
            {
                "errors": [
                    {"message": "Search query throttled"}
                ]
            }
        """.trimIndent()
        val repo = FakeRepository(Result.success(errorJson))
        val result = LandingPageAlbumSearch(search = "spam", repository = repo)
        assertTrue(result.isFailure)
        assertEquals("Search query throttled", result.exceptionOrNull()?.message)
    }

    @Test
    fun `LandingPageAlbumSearch parses valid search landing data`() = runBlocking {
        val validJson = """
            {
                "data": {
                    "landing_page_album": {
                        "search": {
                            "title": "Search: Gothic",
                            "sections": []
                        }
                    }
                }
            }
        """.trimIndent()
        val repo = FakeRepository(Result.success(validJson))
        val result = LandingPageAlbumSearch(search = "gothic", repository = repo)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("Search: Gothic", data.title)
        assertTrue(data.sections.isEmpty())
    }
}
