package com.client.xvideos.r.common.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Каталог ниш R перекачивается только когда кэш на диске устарел. Раньше
 * проверка смотрела на список в памяти, а зовут её сразу после запуска чтения с
 * диска: список ещё пуст, и каждый холодный старт качал каталог заново.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RSavedNichesCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val pageRequests = mutableListOf<Int>()
    private var respond: (page: Int) -> Result<NichesResponse> = { page ->
        Result.success(NichesResponse(niches = listOf(Niche(id = "fresh$page", name = "fresh$page")), page = page, pages = 1))
    }

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_niches")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun cacheFile() = File(AppPath.r_nichesCache, "niches.json")

    private fun writeCache(vararg names: String, ageHours: Long = 0) {
        val file = cacheFile()
        file.parentFile?.mkdirs()
        file.writeText(AppJson.encodeToString(names.map { Niche(id = it, name = it) }))
        file.setLastModified(System.currentTimeMillis() - ageHours * 60 * 60 * 1000)
    }

    private fun kotlinx.coroutines.test.TestScope.cache() = R_Saved_NichesCaches(
        scope = this,
        redApi = RedApi(AppFileDatabase()),
        ioDispatcher = dispatcher,
        loadPage = { page ->
            pageRequests += page
            respond(page)
        },
    )

    @Test
    fun `свежий кэш на диске не перекачивается при запуске`() = runTest(dispatcher) {
        writeCache("cached1", "cached2")
        val cache = cache()

        // Как на старте приложения: проверка идёт сразу за запуском чтения с диска.
        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals("каталог запрошен из сети при свежем кэше", emptyList<Int>(), pageRequests)
        assertEquals(listOf("cached1", "cached2"), cache.list.map { it.name })
    }

    @Test
    fun `кэш старше суток обновляется`() = runTest(dispatcher) {
        writeCache("cached1", ageHours = 25)
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf(1), pageRequests)
        assertEquals(listOf("fresh1"), cache.list.map { it.name })
    }

    @Test
    fun `без файла кэша каталог загружается`() = runTest(dispatcher) {
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf(1), pageRequests)
        assertEquals(listOf("fresh1"), cache.list.map { it.name })
    }

    @Test
    fun `повреждённый файл кэша заменяется свежим каталогом`() = runTest(dispatcher) {
        cacheFile().apply { parentFile?.mkdirs() }.writeText("{ это не список")
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf(1), pageRequests)
        assertEquals(listOf("fresh1"), cache.list.map { it.name })
    }

    @Test
    fun `сбой обновления оставляет прежний каталог и не пишет его на диск`() = runTest(dispatcher) {
        writeCache("cached1", ageHours = 25)
        respond = { Result.failure(IOException("обрыв")) }
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf("cached1"), cache.list.map { it.name })
        assertEquals(false, cache.isDownloading)
    }

    @Test
    fun `ниша, пришедшая на двух страницах, попадает в каталог один раз`() = runTest(dispatcher) {
        // Порядок на сервере сдвинулся между запросами страниц: «b» пришла дважды.
        respond = { page ->
            val ids = if (page == 1) listOf("a", "b") else listOf("b", "c")
            Result.success(NichesResponse(niches = ids.map { Niche(id = it, name = it) }, page = page, pages = 2))
        }
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), cache.list.map { it.id })
        val onDisk = AppJson.decodeFromString<List<Niche>>(cacheFile().readText())
        assertEquals(listOf("a", "b", "c"), onDisk.map { it.id })
    }

    @Test
    fun `повторы из прежнего кэша на диске в список не попадают`() = runTest(dispatcher) {
        writeCache("a", "b", "b", "c")
        val cache = cache()

        cache.refreshIfStale()
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), cache.list.map { it.id })
    }

    @Test
    fun `чтение кэша, начатое ещё в конструкторе, находит файл`() = runTest(dispatcher) {
        writeCache("cached1")

        // Диспетчер без очереди: чтение выполняется сразу, до конца конструктора.
        val cache = R_Saved_NichesCaches(
            scope = this,
            redApi = RedApi(AppFileDatabase()),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            loadPage = { page ->
                pageRequests += page
                respond(page)
            },
        )
        advanceUntilIdle()

        assertEquals(listOf("cached1"), cache.list.map { it.name })
    }
}
