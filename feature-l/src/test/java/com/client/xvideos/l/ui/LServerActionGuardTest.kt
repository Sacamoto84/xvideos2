package com.client.xvideos.l.ui

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.repository.offlineRepository
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.ScreenLSubscribedAlbumsSM
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Лайк и снятие лайка на сервере: повторное нажатие, пока запрос в пути, не
 * должно слать второй. Раньше второй запрос на снятие отвечал ошибкой, и за
 * «Лайк удалён» следом шло «Не удалось удалить лайк».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LServerActionGuardTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Сервер избранного: запрос висит, пока тест его не отпустит; вызовы считаются. */
    private class GatedFavorites : LusciousServerFavoritesRepository {
        val gate = CompletableDeferred<Unit>()
        val addCalls = AtomicInteger()
        val removeCalls = AtomicInteger()

        override suspend fun getSessionUserId(): Result<String> = Result.success("1")
        override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> =
            Result.success(if (page == 1) listOf(AlbumDetails(id = "a1")) else emptyList())

        override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> = Result.success(emptyList())

        override suspend fun addFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> {
            addCalls.incrementAndGet()
            gate.await()
            return Result.success(Unit)
        }

        override suspend fun removeFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> {
            removeCalls.incrementAndGet()
            gate.await()
            return Result.success(Unit)
        }

        override suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String) = Result.success("1")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val favorites = GatedFavorites()

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_l_actions")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        favorites.gate.complete(Unit)
        scope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    private fun expandMenu(): ExpandMenuViewModel {
        val db = AppFileDatabase()
        val luscious = Luscious(scope, offlineRepository(db))
        return ExpandMenuViewModel(
            luscious = luscious,
            saved = SavedL(db, scope, luscious),
            serverFavorites = favorites,
            scope = scope,
            context = ContextWrapper(null),
        )
    }

    @Test
    fun `повторное снятие лайка, пока первое в пути, не шлёт второй запрос`() = runBlocking {
        val menu = expandMenu()
        val picture = PicsDetails(id = "55")

        menu.unlikeOnServer(picture)
        awaitUntil { favorites.removeCalls.get() == 1 }
        menu.unlikeOnServer(picture)
        menu.unlikeOnServer(picture)
        delay(200)

        assertEquals(1, favorites.removeCalls.get())
    }

    @Test
    fun `повторный лайк, пока первый в пути, не шлёт второй запрос`() = runBlocking {
        val menu = expandMenu()
        val picture = PicsDetails(id = "56")

        menu.likeOnServer(picture)
        awaitUntil { favorites.addCalls.get() == 1 }
        menu.likeOnServer(picture)
        delay(200)

        assertEquals(1, favorites.addCalls.get())
    }

    @Test
    fun `после завершения запроса лайк той же картинки можно поставить снова`() = runBlocking {
        val menu = expandMenu()
        val picture = PicsDetails(id = "57")
        favorites.gate.complete(Unit)

        menu.likeOnServer(picture)
        awaitUntil { favorites.addCalls.get() == 1 }
        delay(100)
        menu.likeOnServer(picture)

        awaitUntil { favorites.addCalls.get() == 2 }
    }

    @Test
    fun `повторная отписка от альбома, пока первая в пути, не шлёт второй запрос`() = runBlocking {
        val sm = ScreenLSubscribedAlbumsSM(favorites)
        awaitUntil { sm.albums.value.isNotEmpty() }
        val album = sm.albums.value.single()

        sm.unlikeAlbum(album)
        sm.unlikeAlbum(album)
        delay(200)

        assertEquals(1, favorites.removeCalls.get())
        favorites.gate.complete(Unit)
        awaitUntil { sm.albums.value.isEmpty() }
    }
}
