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
    fun `an ambiguous network failure invalidates the cached server state`() = runTest {
        val writes = NServerFavoriteWrites { _, _, _ -> NNetworkResult.Failure(IOException("connection lost")) }
        val update = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { writes.updates.first() }

        assertTrue(writes.set(42, false, "test").isFailure)
        assertEquals(1L, writes.revision.value)
        assertNull(update.await().saved)
    }
}
