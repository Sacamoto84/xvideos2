package com.client.xvideos.r.common.saved

import android.content.ContextWrapper
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Лента подписок R: отказ сети — это ошибка, а не «у авторов нет роликов».
 * Раньше сбой загрузки каждого автора проглатывался, и без сети лента
 * выглядела пустой, без сообщения и кнопки повтора.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RSubscriptionsFeedTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val notices = mutableListOf<String>()
    private var loadCreator: suspend (name: String) -> List<GifsInfo> = { name -> listOf(GifsInfo(id = "gif_$name", userName = name)) }

    @Before
    fun setUp() {
        val tempDir = tmp.newFolder("app_r_subscriptions")
        val context = object : ContextWrapper(null) {
            override fun getFilesDir(): File = File(tempDir, "files").apply { mkdirs() }
            override fun getCacheDir(): File = File(tempDir, "cache").apply { mkdirs() }
        }
        AppPath.init(context)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun kotlinx.coroutines.test.TestScope.subscriptions(vararg creators: String): R_Saved_Subscriptions {
        val store = R_Saved_Subscriptions(
            scope = backgroundScope,
            redApi = RedApi(AppFileDatabase()),
            loadCreatorGifs = { name -> loadCreator(name) },
            notifyPartialFailure = { notices += it },
        )
        creators.forEach { store.selectedListCreator.add(SelectedCreator(it, true, null)) }
        return store
    }

    @Test
    fun `без сети лента подписок — ошибка, а не пустой список`() = runTest {
        loadCreator = { throw IOException("нет сети") }
        val store = subscriptions("a", "b")

        val error = runCatching { store.loadSubscriptionFeed(listOf("a", "b")) }.exceptionOrNull()

        assertTrue("ожидался отказ сети, пришло $error", error is IOException)
    }

    @Test
    fun `сбой части авторов отдаёт остальных и сообщает о пропуске`() = runTest {
        loadCreator = { name -> if (name == "b") throw IOException("обрыв") else listOf(GifsInfo(id = "gif_$name", userName = name)) }
        val store = subscriptions("a", "b", "c")

        val feed = store.loadSubscriptionFeed(listOf("a", "b", "c"))

        assertEquals(listOf("gif_a", "gif_c"), feed.map { it.id })
        assertEquals(listOf("Лента подписок: не загрузились ролики 1 из 3 авторов"), notices)
    }

    @Test
    fun `ролики одного id от разных авторов в ленте не повторяются`() = runTest {
        loadCreator = { listOf(GifsInfo(id = "same", userName = it)) }
        val store = subscriptions("a", "b")

        val feed = store.loadSubscriptionFeed(listOf("a", "b"))

        assertEquals(listOf("same"), feed.map { it.id })
        assertTrue(notices.isEmpty())
    }

    @Test
    fun `авторы запрашиваются по несколько сразу, но не все разом`() = runTest {
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        val started = AtomicInteger()
        val release = CompletableDeferred<Unit>()
        loadCreator = { name ->
            val now = inFlight.incrementAndGet()
            maxInFlight.updateAndGet { maxOf(it, now) }
            started.incrementAndGet()
            release.await()
            inFlight.decrementAndGet()
            listOf(GifsInfo(id = "gif_$name", userName = name))
        }
        val names = (1..10).map { "c$it" }
        val store = subscriptions(*names.toTypedArray())

        val feed = async { store.loadSubscriptionFeed(names) }
        testScheduler.advanceUntilIdle()
        val startedBeforeRelease = started.get()
        release.complete(Unit)

        assertEquals(10, feed.await().size)
        assertTrue("авторы грузились строго по одному", startedBeforeRelease > 1)
        assertEquals(SUBSCRIPTIONS_PARALLEL_REQUESTS, maxInFlight.get())
    }
}
