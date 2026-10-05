package com.client.xvideos.l.ui

import android.content.ContextWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.navigation.NavigationDepthState
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.l.LServerErrorException
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.AlbumTopHitsImpl
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTagSM
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.ScreenLAlbumSearchSM
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.ScreenLServerLikesSM
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.ScreenLSubscribedAlbumsSM
import com.client.xvideos.l.ui.screens.screenAlbumList.ScreenLAlbumListSM
import com.client.xvideos.l.ui.screens.screenAlbumList.StatusAlbumList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Collections

/**
 * Отказ сети на экранах L доходит до пользователя понятным текстом, а не
 * только до logcat. Раньше список альбомов показывал пустую сетку, а топ,
 * поиск, тег и дозагрузка лайков с подписками молчали.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LScreensErrorReportingTest {

    companion object {
        private const val SERVER_DOWN = "Сервер L недоступен (HTTP 500)"

        @BeforeClass
        @JvmStatic
        fun setUpPaths() {
            val tempDir = Files.createTempDirectory("app_path_test_l_errors").toFile()
            val context = object : ContextWrapper(null) {
                override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
                override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
            }
            AppPath.init(context)
        }
    }

    /** Сервер L лежит: любой запрос — 500. Считает запросы списка альбомов. */
    private class DownRepository : Repository(AppFileDatabase()) {
        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> =
            Result.failure(LServerErrorException(500))
    }

    /** Сервер L лежит, пока тест его не поднимет; после этого отдаёт [body]. */
    private class RecoveringRepository(private val body: String) : Repository(AppFileDatabase()) {
        @Volatile
        var up = false

        override suspend fun openURI(data: String, config: RepositoryUriConfig): Result<String> =
            if (up) Result.success(body) else Result.failure(LServerErrorException(500))
    }

    /** Первая страница есть, следующая падает с 500. */
    private class SecondPageDownFavorites : LusciousServerFavoritesRepository {
        override suspend fun getSessionUserId(): Result<String> = Result.success("1")
        override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> =
            if (page == 1) Result.success(listOf(AlbumDetails(id = "a1"))) else Result.failure(LServerErrorException(500))
        override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> =
            if (page == 1) Result.success(listOf(PicsDetails(id = "p1"))) else Result.failure(LServerErrorException(500))
        override suspend fun addFavorite(anchorId: String, anchorType: String, favoriteType: String) = Result.success(Unit)
        override suspend fun removeFavorite(anchorId: String, anchorType: String, favoriteType: String) = Result.success(Unit)
        override suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String) = Result.success("1")
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
    private val snackbarErrors: MutableList<String> = Collections.synchronizedList(mutableListOf())

    /**
     * ScreenModel, созданные без навигатора, Voyager селит в один общий scope
     * `standalone`. Отменять его нельзя — он нужен ScreenModel следующих
     * тестов, поэтому tearDown только дожидается корутин этого теста.
     */
    private lateinit var standaloneScopeJob: Job
    private lateinit var jobsBeforeTest: Set<Job>
    private lateinit var collectorJob: Job

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        standaloneScopeJob = object : ScreenModel {}.screenModelScope.coroutineContext.job
        jobsBeforeTest = standaloneScopeJob.children.toSet()
        collectorJob = scope.launch {
            EventBus.events.collect { event ->
                val message = (event as? Event.ShowSnackBar)?.message
                if (message is UiMessage.Error) snackbarErrors += message.text
            }
        }
    }

    @After
    fun tearDown() {
        // Отставшие корутины экранов (например, загрузка фильтров в init
        // списка альбомов) иначе возвращались на Main уже после resetMain и
        // роняли чужой тест «uncaught exceptions before the test started».
        runBlocking {
            withTimeout(5_000) {
                (standaloneScopeJob.children.toSet() - jobsBeforeTest).forEach { it.join() }
                scope.coroutineContext.job.children.filter { it !== collectorJob }.forEach { it.join() }
            }
        }
        scope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    private fun luscious() = Luscious(scope, DownRepository())

    @Test
    fun `список альбомов показывает ошибку на странице вместо пустой сетки`() = runBlocking {
        val sm = ScreenLAlbumListSM(inFilter = null, luscious = luscious())

        sm.loadAlbumList(0)
        awaitUntil { sm.bigList[0]?.status == StatusAlbumList.ERROR }

        assertEquals(SERVER_DOWN, sm.bigList[0]?.errorMessage)
    }

    @Test
    fun `первая загрузка списка после фильтра тоже оставляет ошибку на странице`() = runBlocking {
        val sm = ScreenLAlbumListSM(inFilter = null, luscious = luscious())

        sm.loadInitialData()
        awaitUntil { sm.bigList[0]?.status == StatusAlbumList.ERROR }

        assertEquals(SERVER_DOWN, sm.bigList[0]?.errorMessage)
    }

    @Test
    fun `топ сообщает об отказе`() = runBlocking {
        val top = AlbumTopHitsImpl(repository = DownRepository(), scope = scope)

        awaitUntil { top.loadError.value == SERVER_DOWN }
    }

    @Test
    fun `топ загружается повтором после отказа`() = runBlocking {
        val repository = RecoveringRepository("""{"data":{"album":{"list_top_hits":[{"title":"Top 1"}]}}}""")
        val top = AlbumTopHitsImpl(repository = repository, scope = scope)
        awaitUntil { top.loadError.value == SERVER_DOWN }

        repository.up = true
        top.reload()

        awaitUntil { top.items.map { it.title } == listOf("Top 1") }
        assertEquals(null, top.loadError.value)
    }

    @Test
    fun `поиск сообщает об отказе`() = runBlocking {
        val sm = ScreenLAlbumSearchSM(luscious())
        sm.updateSearchText("abc")

        sm.search()

        awaitUntil { SERVER_DOWN in snackbarErrors }
    }

    @Test
    fun `тег сообщает об отказе`() = runBlocking {
        val sm = ScreenLAlbumLandingTagSM(tag = "x", luscious = luscious(), depthState = NavigationDepthState())

        awaitUntil { sm.loadError.value == SERVER_DOWN }
    }

    @Test
    fun `страница тега загружается повтором после отказа`() = runBlocking {
        val repository = RecoveringRepository(
            """{"data":{"landing_page_album":{"tag":{"title":"Tag X","sections":[{"title":"S1","items":[]}]}}}}"""
        )
        val sm = ScreenLAlbumLandingTagSM(
            tag = "x",
            luscious = Luscious(scope, repository),
            depthState = NavigationDepthState(),
        )
        awaitUntil { sm.loadError.value == SERVER_DOWN }

        repository.up = true
        sm.retry()

        awaitUntil { sm.albumTopHits.value?.title == "Tag X" }
        assertEquals(null, sm.loadError.value)
    }

    @Test
    fun `показать все в поиске строит фильтр по запросу, по которому искали`() = runBlocking {
        val sm = ScreenLAlbumSearchSM(luscious())
        sm.updateSearchText("abc")
        sm.search()
        awaitUntil { SERVER_DOWN in snackbarErrors }

        // Поле уже правят под следующий запрос, а на экране — результаты прежнего.
        sm.updateSearchText("xyz")

        assertEquals("abc", sm.searchedQuery)
        assertEquals("abc", sm.createFilter(Landing_page_albumSection(title = "Manga")).searchQuery)
    }

    @Test
    fun `дозагрузка серверных лайков сообщает об отказе`() = runBlocking {
        val sm = ScreenLServerLikesSM(SecondPageDownFavorites())
        awaitUntil { sm.pictures.value.isNotEmpty() && !sm.isLoading.value }

        sm.loadNextPage()

        awaitUntil { SERVER_DOWN in snackbarErrors }
    }

    @Test
    fun `дозагрузка подписок сообщает об отказе`() = runBlocking {
        val sm = ScreenLSubscribedAlbumsSM(SecondPageDownFavorites())
        awaitUntil { sm.albums.value.isNotEmpty() && !sm.isLoading.value }

        sm.loadNextPage()

        awaitUntil { SERVER_DOWN in snackbarErrors }
    }
}
