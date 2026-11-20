package com.client.xvideos.l.featured.saved

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LMediaStreamingTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `первая порция пишется на диск до окончания ответа`() = runBlocking {
        val destination = tmp.root.resolve("clip.mp4")
        val body = ByteChannel(autoFlush = true)
        val firstWritten = CompletableDeferred<Unit>()
        HttpClient(MockEngine { respond(body) }).use { client ->
            val download = async(Dispatchers.IO) {
                lDownloadToFile(client, "https://example.com/clip.mp4", destination) { _, _ ->
                    firstWritten.complete(Unit)
                }
            }
            try {
                body.writeStringUtf8("first")
                withTimeout(5_000) { firstWritten.await() }
                assertEquals("first", tmp.root.resolve("clip.mp4.part").readText())
                assertFalse(destination.exists())
                body.writeStringUtf8("second")
            } finally {
                body.flushAndClose()
            }
            download.await()
            assertEquals("firstsecond", destination.readText())
        }
    }
}
