package com.client.xvideos.l.ui.screens.explorer.tab.saved

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.ScreenLServerLikesSM
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.ScreenLSubscribedAlbumsSM
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LusciousPaginationRaceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestFavoritesRepo : LusciousServerFavoritesRepository {
        var page2AlbumDeferred: CompletableDeferred<Result<List<AlbumDetails>>>? = null
        var refreshedAlbumList = listOf(AlbumDetails(id = "refreshed_1", title = "Refreshed Album"))

        var page2PicDeferred: CompletableDeferred<Result<List<PicsDetails>>>? = null
        var refreshedPicList = listOf(PicsDetails(id = "refreshed_pic_1"))

        override suspend fun getSessionUserId(): Result<String> = Result.success("12345")

        override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> =
            Result.success("{}")

        override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> {
            return when (page) {
                1 -> Result.success(refreshedAlbumList)
                2 -> page2AlbumDeferred?.await() ?: Result.success(emptyList())
                else -> Result.success(emptyList())
            }
        }

        override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> =
            Result.success("{}")

        override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> {
            return when (page) {
                1 -> Result.success(refreshedPicList)
                2 -> page2PicDeferred?.await() ?: Result.success(emptyList())
                else -> Result.success(emptyList())
            }
        }

        override suspend fun addFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun removeFavorite(anchorId: String, anchorType: String, favoriteType: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String): Result<String> =
            Result.success("123")
    }

    @Test
    fun `ScreenLSubscribedAlbumsSM refresh cancels pending loadNextPage`() = runTest {
        val repo = TestFavoritesRepo()
        repo.refreshedAlbumList = listOf(AlbumDetails(id = "1", title = "Initial 1"))
        val sm = ScreenLSubscribedAlbumsSM(repo)
        advanceUntilIdle()

        assertEquals(1, sm.albums.value.size)
        assertEquals("Initial 1", sm.albums.value.first().title)

        // Подготавливаем заблокированный deferred для page 2
        val page2Deferred = CompletableDeferred<Result<List<AlbumDetails>>>()
        repo.page2AlbumDeferred = page2Deferred

        sm.loadNextPage()
        testDispatcher.scheduler.runCurrent()
        assertTrue(sm.isLoading.value)

        // Вызываем refresh() с новыми данными до завершения page 2
        repo.refreshedAlbumList = listOf(AlbumDetails(id = "refreshed_10", title = "Fresh 10"))
        sm.refresh()
        testDispatcher.scheduler.runCurrent()

        // Теперь завершаем отменённый page 2
        page2Deferred.complete(Result.success(listOf(AlbumDetails(id = "stale_2", title = "Stale 2"))))
        advanceUntilIdle()

        assertFalse(sm.isLoading.value)
        assertFalse(sm.isRefreshing.value)
        // В списке должен быть только результат refresh(), stale_2 не должен попасть
        assertEquals(1, sm.albums.value.size)
        assertEquals("refreshed_10", sm.albums.value.first().id)
    }

    @Test
    fun `ScreenLServerLikesSM refresh cancels pending loadNextPage`() = runTest {
        val repo = TestFavoritesRepo()
        repo.refreshedPicList = listOf(PicsDetails(id = "initial_p1"))
        val sm = ScreenLServerLikesSM(repo)
        advanceUntilIdle()

        assertEquals(1, sm.pictures.value.size)
        assertEquals("initial_p1", sm.pictures.value.first().id)

        // Подготавливаем заблокированный deferred для page 2
        val page2Deferred = CompletableDeferred<Result<List<PicsDetails>>>()
        repo.page2PicDeferred = page2Deferred

        sm.loadNextPage()
        testDispatcher.scheduler.runCurrent()
        assertTrue(sm.isLoading.value)

        // Вызываем refresh() с новыми данными до завершения page 2
        repo.refreshedPicList = listOf(PicsDetails(id = "fresh_p10"))
        sm.refresh()
        testDispatcher.scheduler.runCurrent()

        // Завершаем отменённый page 2
        page2Deferred.complete(Result.success(listOf(PicsDetails(id = "stale_p2"))))
        advanceUntilIdle()

        assertFalse(sm.isLoading.value)
        assertFalse(sm.isRefreshing.value)
        // В списке должен быть только результат refresh(), stale_p2 не должен попасть
        assertEquals(1, sm.pictures.value.size)
        assertEquals("fresh_p10", sm.pictures.value.first().id)
    }
}
