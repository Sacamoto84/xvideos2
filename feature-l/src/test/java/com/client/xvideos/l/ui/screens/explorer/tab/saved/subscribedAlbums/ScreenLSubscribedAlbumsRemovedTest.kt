package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.repository.LusciousServerFavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Альбом, удалённый с сервера на экране альбома, пропадает из списка подписок
 * сразу. Раньше он оставался в списке до ручного обновления.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenLSubscribedAlbumsRemovedTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FavoritesWithRemovals : LusciousServerFavoritesRepository {
        override val removedAlbumIds = MutableSharedFlow<String>(extraBufferCapacity = 4)

        override suspend fun getSessionUserId(): Result<String> = Result.success("1")
        override suspend fun getSubscribedAlbumsRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getServerLikedPicturesRaw(userId: String?, page: Int): Result<String> = Result.success("{}")
        override suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>> =
            Result.success(if (page == 1) listOf(AlbumDetails(id = "a1"), AlbumDetails(id = "a2")) else emptyList())

        override suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>> = Result.success(emptyList())
        override suspend fun addFavorite(anchorId: String, anchorType: String, favoriteType: String) = Result.success(Unit)
        override suspend fun removeFavorite(anchorId: String, anchorType: String, favoriteType: String) =
            Result.success(Unit)

        override suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String) = Result.success("1")
    }

    @Test
    fun `альбом, удалённый с сервера в другом месте, уходит из списка подписок`() = runTest {
        val repo = FavoritesWithRemovals()
        val sm = ScreenLSubscribedAlbumsSM(repo)
        advanceUntilIdle()
        assertEquals(listOf("a1", "a2"), sm.albums.value.map { it.id })

        repo.removedAlbumIds.emit("a1")
        advanceUntilIdle()

        assertEquals(listOf("a2"), sm.albums.value.map { it.id })
        sm.onDispose()
    }

    @Test
    fun `удаление альбома не из списка список не трогает`() = runTest {
        val repo = FavoritesWithRemovals()
        val sm = ScreenLSubscribedAlbumsSM(repo)
        advanceUntilIdle()

        repo.removedAlbumIds.emit("other")
        advanceUntilIdle()

        assertEquals(listOf("a1", "a2"), sm.albums.value.map { it.id })
        sm.onDispose()
    }
}
