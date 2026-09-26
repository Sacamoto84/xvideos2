package com.client.xvideos.common

import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.json.decodeFromStringOrNull
import com.client.xvideos.common.json.encodeToStringOrNull
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.common.util.runCatchingCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Batch47CoreTest {

    @Serializable
    private data class DummyPayload(val id: Int, val name: String)

    @Test
    fun `runCatchingCancellable with receiver works for success, failure and rethrows cancellation`() {
        val str = "hello"
        val successRes = str.runCatchingCancellable { length }
        assertTrue(successRes.isSuccess)
        assertEquals(5, successRes.getOrNull())

        val failRes = str.runCatchingCancellable { error("boom") }
        assertTrue(failRes.isFailure)

        var thrown = false
        try {
            str.runCatchingCancellable { throw CancellationException("cancel") }
        } catch (_: CancellationException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `launchCatching onError callback executes on exception`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)

        var caughtError: Exception? = null
        scope.launchCatching(
            message = "test failure",
            onError = { caughtError = it }
        ) {
            throw IllegalStateException("something failed")
        }
        testScheduler.advanceUntilIdle()
        assertNotNull(caughtError)
        assertEquals("something failed", caughtError?.message)
    }

    @Test
    fun `Event inspection properties categorize events correctly`() {
        val snackbarEvent = Event.ShowSnackBar(UiMessage.Error("err"))
        assertTrue(snackbarEvent.isSnackBar)
        assertFalse(snackbarEvent.isLog)
        assertFalse(snackbarEvent.isExitPosition)
        assertTrue(snackbarEvent.isError)
        assertFalse(snackbarEvent.isSuccess)
        assertFalse(snackbarEvent.isInfo)
        assertFalse(snackbarEvent.isWarning)

        val logEvent = Event.Log("log message")
        assertFalse(logEvent.isSnackBar)
        assertTrue(logEvent.isLog)

        val exitEvent = Event.X_FullScreenExitPosition(12345L)
        assertTrue(exitEvent.isExitPosition)
    }

    @Test
    fun `EventBus exposes subscriberCount and hasSubscribers properties`() {
        assertEquals(0, EventBus.subscriberCount)
        assertFalse(EventBus.hasSubscribers)
    }

    @Test
    fun `AppJson decodeFromStringOrNull and encodeToStringOrNull handle null and invalid strings safely`() {
        assertNull(AppJson.decodeFromStringOrNull<DummyPayload>(null))
        assertNull(AppJson.decodeFromStringOrNull<DummyPayload>(""))
        assertNull(AppJson.decodeFromStringOrNull<DummyPayload>("   "))
        assertNull(AppJson.decodeFromStringOrNull<DummyPayload>("invalid json"))

        val validJson = """{"id":1,"name":"test"}"""
        val decoded = AppJson.decodeFromStringOrNull<DummyPayload>(validJson)
        assertNotNull(decoded)
        assertEquals(1, decoded?.id)
        assertEquals("test", decoded?.name)

        val encoded = AppJson.encodeToStringOrNull(DummyPayload(2, "two"))
        assertNotNull(encoded)
        assertTrue(encoded!!.contains(""""id": 2""") || encoded.contains(""""id":2"""))
    }
}
