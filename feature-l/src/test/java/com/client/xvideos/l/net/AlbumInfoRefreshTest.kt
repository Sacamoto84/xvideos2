package com.client.xvideos.l.net

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.graphQl.getAlbumInfo
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.HTML_INSTEAD_OF_JSON_PREFIX
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.UnknownHostException
import java.nio.file.Files
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumInfoRefreshTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            val tempDir = Files.createTempDirectory("app_path_test_album_info").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private const val FRESH_PICS = 2

        private fun albumJson(id: Int, title: String) = """{"data":{"album":{"get":{"id":"$id","title":"$title"}}}}"""

        private val PAGE_JSON: String = run {
            val items = (1..FRESH_PICS).joinToString(",") { n ->
                """{"id":"$n","url_to_original":"https://example.com/fresh-$n.jpg"}"""
            }
            val info = """{"total_pages":1,"total_items":$FRESH_PICS,"items_per_page":30}"""
            """{"data":{"picture":{"list":{"info":$info,"items":[$items]}}}}"""
        }
    }

    /** Сервер альбома: метаданные и единственная страница картинок отвечают так, как велит тест. */
    private class AlbumRepository(private val albumId: Int) : Repository(AppFileDatabase()) {
        @Volatile
        var albumInfo: Result<String> = Result.success(albumJson(albumId, "Fresh Album"))

        /** Сколько раз запрошены метаданные. */
        val albumInfoRequests = MutableStateFlow(0)

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> {
            if (data != getAlbumInfo(albumId)) return Result.success(PAGE_JSON)
            albumInfoRequests.update { it + 1 }
            return albumInfo
        }
    }

    private suspend fun Repository.putCachedAlbum(albumId: Int, title: String) {
        val bundle = LAlbumBundleCache(
            schemaVersion = L_ALBUM_BUNDLE_CACHE_SCHEMA_VERSION,
            cachedAtMs = System.currentTimeMillis(),
            album = AlbumDetails(id = albumId.toString(), title = title, download_url = ""),
            totalPages = 1,
            pics = listOf(PicsDetails(height = 100, width = 100, url_to_original = "https://example.com/cached.jpg"))
        )
        putAlbumBundleCache(albumId, LJson.encodeToString(bundle))
    }

    private suspend fun Repository.cachedAlbum(albumId: Int): LAlbumBundleCache? =
        getAlbumBundleCache(albumId, L_ALBUM_BUNDLE_CACHE_MAX_AGE_MS)?.let { LJson.decodeFromString<LAlbumBundleCache>(it) }

    /** Обновляет альбом и ждёт конца: сначала запроса к серверу, затем снятия признака. */
    private suspend fun AlbumInfo.refreshAndAwait(repository: AlbumRepository) {
        val requestsBefore = repository.albumInfoRequests.value
        refresh()
        withTimeout(5_000) {
            repository.albumInfoRequests.first { it > requestsBefore }
            isRefreshing.first { !it }
        }
    }

    @Test
    fun `успешное обновление заменяет кэш альбома свежим`() = runBlocking {
        val albumId = 98765
        val repository = AlbumRepository(albumId)
        repository.putCachedAlbum(albumId, "Cached Album")
        val albumInfo = AlbumInfo(id = albumId, repository = repository, scope = this)
        withTimeout(5_000) { albumInfo.albumInfo.first { it != null } }
        assertEquals("Cached Album", albumInfo.albumInfo.value?.title)

        albumInfo.refreshAndAwait(repository)

        assertEquals("Fresh Album", albumInfo.albumInfo.value?.title)
        val cached = repository.cachedAlbum(albumId)
        assertEquals("Fresh Album", cached?.album?.title)
        assertEquals(FRESH_PICS, cached?.pics?.size)
    }

    @Test
    fun `неудачное обновление оставляет кэш и альбом на экране и сообщает об ошибке`() = runBlocking {
        val albumId = 98766
        val repository = AlbumRepository(albumId)
        repository.putCachedAlbum(albumId, "Cached Album")
        val notices: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val albumInfo = AlbumInfo(id = albumId, repository = repository, scope = this, notifyRefreshFailed = { notices += it })
        withTimeout(5_000) { albumInfo.albumInfo.first { it != null } }

        repository.albumInfo = Result.failure(UnknownHostException("api.example.com"))
        albumInfo.refreshAndAwait(repository)

        assertNotNull("кэш альбома стёрт неудачным обновлением", repository.cachedAlbum(albumId))
        assertEquals("Cached Album", albumInfo.albumInfo.value?.title)
        assertEquals(1, albumInfo.albumPicsDetails.pics.size)
        assertEquals(listOf("Нет связи с сервером L: api.example.com"), notices.toList())
        assertNull("альбом на экране есть — экран ошибки не нужен", albumInfo.loadError.value)
    }

    @Test
    fun `обновление поднимает признак сразу, не дожидаясь старта загрузки`() = runTest {
        val albumId = 98767
        // Загрузки стоят в очереди планировщика и не стартуют: признак должен
        // подняться самим вызовом, иначе второй жест подряд проходит проверку.
        val albumInfo = AlbumInfo(
            id = albumId,
            repository = AlbumRepository(albumId),
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        albumInfo.refresh()

        assertTrue(albumInfo.isRefreshing.value)
    }

    @Test
    fun `обновление, отменённое до старта, не оставляет признак поднятым`() = runTest {
        val albumId = 98769
        val albumInfo = AlbumInfo(
            id = albumId,
            repository = AlbumRepository(albumId),
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        albumInfo.refresh()
        // «Повторить» на экране ошибки отменяет обновление, которое ещё не начало
        // выполняться: его тело не запускается вовсе.
        albumInfo.retry()
        runCurrent()

        assertFalse("признак обновления остался поднятым навсегда", albumInfo.isRefreshing.value)
    }

    @Test
    fun `ошибка загрузки альбома показывается текстом для пользователя`() = runBlocking {
        val albumId = 98768
        val repository = AlbumRepository(albumId)
        repository.albumInfo = Result.failure(
            IllegalStateException("$HTML_INSTEAD_OF_JSON_PREFIX: <html><body>Just a moment</body></html>")
        )

        val albumInfo = AlbumInfo(id = albumId, repository = repository, scope = this)
        val error = withTimeout(5_000) { albumInfo.loadError.first { it != null } }.orEmpty()

        assertFalse(error, error.contains("<html"))
        assertFalse(error, error.startsWith(HTML_INSTEAD_OF_JSON_PREFIX))
        assertFalse("загрузка должна завершиться", withTimeout(5_000) { albumInfo.isLoading.first { !it } })
    }

    @Test
    fun `downloadUrl returns empty string when download_url is missing`() = runBlocking {
        val albumId = 112233
        val repository = AlbumRepository(albumId)
        repository.putCachedAlbum(albumId, "No Download")

        val albumInfo = AlbumInfo(id = albumId, repository = repository, scope = this)
        withTimeout(5_000) { albumInfo.albumInfo.first { it != null } }

        assertEquals("", albumInfo.downloadUrl)
    }
}
