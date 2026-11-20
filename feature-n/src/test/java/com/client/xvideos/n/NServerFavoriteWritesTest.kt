package com.client.xvideos.n

import com.client.xvideos.n.net.NNetworkResult
import com.client.xvideos.n.repository.NServerFavoriteWrites
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NServerFavoriteWritesTest {
    @Test
    fun `opposite changes from different screens are sent in order for the same album`() = runTest {
        val firstResponse = CompletableDeferred<Unit>()
        val calls = mutableListOf<Pair<Long, Boolean>>()
        val writes = NServerFavoriteWrites { id, favorite, _ ->
            calls += id to favorite
            if (id == 42L && favorite) firstResponse.await()
            favoriteResponse(favorite)
        }

        val add = async(start = CoroutineStart.UNDISPATCHED) { writes.set(42, true, "key") }
        val remove = async(start = CoroutineStart.UNDISPATCHED) { writes.set(42, false, "key") }
        assertEquals(listOf(42L to true), calls)
        assertEquals(false, writes.set(43, false, "key").getOrThrow())

        firstResponse.complete(Unit)
        assertEquals(true, add.await().getOrThrow())
        assertEquals(false, remove.await().getOrThrow())
        assertEquals(listOf(42L to true, 43L to false, 42L to false), calls)
    }

    @Test
    fun `queued writes are discarded when cancelled or when their account changes`() = runTest {
        val firstResponse = CompletableDeferred<Unit>()
        val calls = mutableListOf<Boolean>()
        var currentKey = "original"
        val writes = NServerFavoriteWrites(isCurrentKey = { it == currentKey }) { _, favorite, _ ->
            calls += favorite
            if (favorite) firstResponse.await()
            favoriteResponse(favorite)
        }

        val first = async(start = CoroutineStart.UNDISPATCHED) { writes.set(42, true, currentKey) }
        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) { writes.set(42, false, currentKey) }
        val signedOut = async(start = CoroutineStart.UNDISPATCHED) { writes.set(42, false, currentKey) }
        cancelled.cancel()
        currentKey = "replacement"
        firstResponse.complete(Unit)

        assertTrue(first.await().isSuccess)
        cancelled.join()
        assertTrue(signedOut.await().isFailure)
        assertEquals(listOf(true), calls)
        assertEquals(1L, writes.revision.value)
    }


    @Test
    fun `a sent change still publishes its result after the screen is closed`() = runTest {
        val sent = CompletableDeferred<Unit>()
        val response = CompletableDeferred<NNetworkResult>()
        val writes = NServerFavoriteWrites { _, _, _ ->
            sent.complete(Unit)
            response.await()
        }
        val update = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { writes.updates.first() }
        val screen = launch { writes.set(42, false, "test") }

        sent.await()
        screen.cancel()
        response.complete(NNetworkResult.Success("""{"favorited":false}""", "https://nhentai.net/"))
        screen.join()

        assertEquals(1L, writes.revision.value)
        assertEquals(42L, update.await().galleryId)
        assertEquals(false, update.await().saved)
    }

    @Test
    fun `rate limited favorites retain the retry message`() = runTest {
        val message = "Слишком много запросов. Повторите через 17 сек."
        val writes = NServerFavoriteWrites { _, _, _ -> NNetworkResult.Error(429, message) }

        assertEquals(message, writes.set(42, true, "test").exceptionOrNull()?.message)
        assertEquals(0L, writes.revision.value)
    }

    @Test
    fun `an ambiguous network failure invalidates the cached server state`() = runTest {
        val writes = NServerFavoriteWrites { _, _, _ -> NNetworkResult.Failure(IOException("connection lost")) }
        val update = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { writes.updates.first() }

        assertTrue(writes.set(42, false, "test").isFailure)
        assertEquals(1L, writes.revision.value)
        assertNull(update.await().saved)
    }

    private fun favoriteResponse(favorite: Boolean) =
        NNetworkResult.Success("""{"favorited":$favorite}""", "https://nhentai.net/")
}
