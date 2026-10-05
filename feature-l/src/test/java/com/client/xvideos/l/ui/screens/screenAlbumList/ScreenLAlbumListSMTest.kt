package com.client.xvideos.l.ui.screens.screenAlbumList

import android.content.ContextWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.net.graphQl.getAlbumListGraphQL1
import com.client.xvideos.l.net.graphQl.getAlbumListWithAggregations
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Collections

/**
 * Смена фильтра в списке альбомов: загрузки прежнего фильтра не должны
 * дописывать свои ответы под новый. Раньше их никто не отменял — страницы и
 * счётчики старого фильтра приходили позже и оставались на экране.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenLAlbumListSMTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpPaths() {
            val tempDir = Files.createTempDirectory("app_path_test_l_album_list").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }

        private val OLD = AlbumListFilter()
        private val NEW = AlbumListFilter(searchQuery = "new")
        private const val OLD_TOTAL_PAGES = 9
        private const val NEW_TOTAL_PAGES = 3
        private const val MAX_PAGE = 10

        private fun listJson(filter: AlbumListFilter, page: Int): String {
            val name = if (filter == OLD) "old" else "new"
            val totalPages = if (filter == OLD) OLD_TOTAL_PAGES else NEW_TOTAL_PAGES
            val info = """{"page":$page,"total_pages":$totalPages,"total_items":${totalPages * 30},"items_per_page":30}"""
            return """{"data":{"album":{"list":{"info":$info,"items":[{"id":"$page","title":"$name $page"}]}}}}"""
        }

        private fun aggregationsJson(genre: String): String {
            val genres = """{"field":{"short_name":"genre_ids"},"values":[{"count":5,"term":"$genre","is_active":false}]}"""
            return """{"data":{"album":{"list_with_aggregations":{"aggregations":[$genres]}}}}"""
        }
    }

    /** Сервер списка альбомов: страницы и счётчики фильтров отвечают так, как велит тест. */
    private class ListRepository : Repository(AppFileDatabase()) {
        @Volatile
        var list: suspend (filter: AlbumListFilter, page: Int) -> Result<String> =
            { filter, page -> Result.success(listJson(filter, page)) }

        @Volatile
        var aggregations: suspend (filter: AlbumListFilter) -> Result<String> =
            { filter -> Result.success(aggregationsJson(if (filter == OLD) "old" else "new")) }

        /** Запрошенные страницы списка: фильтр и номер страницы на сервере (с единицы). */
        val listRequests: MutableList<Pair<AlbumListFilter, Int>> = Collections.synchronizedList(mutableListOf())
        val aggregationRequests: MutableList<AlbumListFilter> = Collections.synchronizedList(mutableListOf())

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> {
            for (filter in listOf(OLD, NEW)) {
                if (data == getAlbumListWithAggregations(1, filter)) {
                    aggregationRequests += filter
                    return aggregations(filter)
                }
                for (page in 1..MAX_PAGE) {
                    if (data == getAlbumListGraphQL1(page, filter)) {
                        listRequests += filter to page
                        return list(filter, page)
                    }
                }
            }
            // Справочник категорий, который фасад L запрашивает при создании.
            return Result.failure(IOException("нет ответа"))
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
    private val repository = ListRepository()

    /**
     * ScreenModel, созданные без навигатора, Voyager селит в один общий scope
     * `standalone`. Отменять его нельзя — он нужен ScreenModel следующих
     * тестов, поэтому тест только дожидается своих корутин.
     */
    private lateinit var standaloneScopeJob: Job
    private lateinit var jobsBeforeTest: Set<Job>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        standaloneScopeJob = object : ScreenModel {}.screenModelScope.coroutineContext.job
        jobsBeforeTest = standaloneScopeJob.children.toSet()
    }

    @After
    fun tearDown() {
        runBlocking { awaitScreenModelJobs() }
        scope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    /** Ждёт, пока корутины экрана, запущенные этим тестом, завершатся — своим ходом или отменой. */
    private suspend fun awaitScreenModelJobs() {
        withTimeout(5_000) {
            while (true) {
                val active = (standaloneScopeJob.children.toSet() - jobsBeforeTest).filter { it.isActive }
                if (active.isEmpty()) break
                active.forEach { it.join() }
            }
            scope.coroutineContext.job.children.forEach { it.join() }
        }
    }

    private fun screenModel(filter: AlbumListFilter = OLD) =
        ScreenLAlbumListSM(inFilter = filter, luscious = Luscious(scope, repository))

    @Test
    fun `после смены фильтра ответ страницы прежнего фильтра в список не попадает`() = runBlocking {
        val oldPageAnswer = CompletableDeferred<Unit>()
        repository.list = { filter, page ->
            if (filter == OLD && page == 8) oldPageAnswer.await()
            Result.success(listJson(filter, page))
        }
        val sm = screenModel()

        // Пользователь долистал до дальней страницы — её запрос ещё в пути.
        sm.loadAlbumList(7)
        awaitUntil { OLD to 8 in repository.listRequests }

        sm.filterUpdate(NEW)
        sm.loadInitialData()
        awaitUntil { sm.bigList[0]?.status == StatusAlbumList.DOWNLOADED }
        oldPageAnswer.complete(Unit)
        awaitScreenModelJobs()

        assertNull("страница прежнего фильтра записана под новым", sm.bigList[7]?.albumListImplInfoAndList)
        assertEquals("число страниц взято из ответа прежнего фильтра", NEW_TOTAL_PAGES, sm.info.value?.totalPages)
        assertEquals("new 1", sm.bigList[0]?.albumListImplInfoAndList?.items?.single()?.title)
    }

    @Test
    fun `смена фильтра сразу убирает сведения о страницах прежнего`() = runBlocking {
        val newPageAnswer = CompletableDeferred<Unit>()
        repository.list = { filter, page ->
            if (filter == NEW) newPageAnswer.await()
            Result.success(listJson(filter, page))
        }
        val sm = screenModel()
        sm.loadAlbumList(0)
        awaitUntil { sm.info.value?.totalPages == OLD_TOTAL_PAGES }

        sm.filterUpdate(NEW)
        sm.loadInitialData()

        assertNull("пока новый фильтр грузится, число страниц прежнего не годится", sm.info.value)
        newPageAnswer.complete(Unit)
        awaitScreenModelJobs()
        assertEquals(NEW_TOTAL_PAGES, sm.info.value?.totalPages)
    }

    @Test
    fun `признак запроса держится, пока грузится хотя бы одна страница`() = runBlocking {
        val secondPageAnswer = CompletableDeferred<Unit>()
        repository.list = { filter, page ->
            if (page == 2) secondPageAnswer.await()
            Result.success(listJson(filter, page))
        }
        val sm = screenModel()

        sm.loadAlbumList(0)
        sm.loadAlbumList(1)
        awaitUntil { sm.bigList[0]?.status == StatusAlbumList.DOWNLOADED && OLD to 2 in repository.listRequests }
        delay(50)

        assertTrue("первая страница загрузилась и погасила признак, хотя вторая ещё грузится", sm.requestsInFlight.value > 0)

        secondPageAnswer.complete(Unit)
        awaitScreenModelJobs()
        assertEquals(0, sm.requestsInFlight.value)
    }

    @Test
    fun `счётчики прежнего фильтра, пришедшие после смены, не затирают счётчики нового`() = runBlocking {
        val oldAggregationsAnswer = CompletableDeferred<Unit>()
        repository.aggregations = { filter ->
            if (filter == OLD) oldAggregationsAnswer.await()
            Result.success(aggregationsJson(if (filter == OLD) "old" else "new"))
        }
        val sm = screenModel()
        awaitUntil { OLD in repository.aggregationRequests }

        sm.filterUpdate(NEW)
        sm.loadInitialData()
        awaitUntil { sm.filterGenreStateCount.value.singleOrNull()?.term == "new" }
        oldAggregationsAnswer.complete(Unit)
        awaitScreenModelJobs()

        assertEquals("new", sm.filterGenreStateCount.value.single().term)
    }

    @Test
    fun `сбой счётчиков нового фильтра не оставляет счётчики прежнего`() = runBlocking {
        repository.aggregations = { filter ->
            if (filter == OLD) Result.success(aggregationsJson("old")) else Result.failure(IOException("обрыв"))
        }
        val sm = screenModel()
        awaitUntil { sm.filterGenreStateCount.value.singleOrNull()?.term == "old" }

        sm.filterUpdate(NEW)
        sm.loadInitialData()
        awaitScreenModelJobs()

        assertTrue(sm.filterGenreStateCount.value.isEmpty())
    }
}
