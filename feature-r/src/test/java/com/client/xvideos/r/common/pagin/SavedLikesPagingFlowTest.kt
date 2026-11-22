package com.client.xvideos.r.common.pagin

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.paging.PagingConfig
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.cachedIn
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class SavedLikesPagingFlowTest {

    @Test
    fun `лайк из другой вкладки появляется при возврате и замена без изменения размера видна сразу`() = runTest {
        val first = GifsInfo(id = "first", createDate = 1)
        val second = GifsInfo(id = "second", createDate = 2)
        val replacement = GifsInfo(id = "replacement", createDate = 3)
        val likes = mutableStateListOf(first)
        val feed = savedLikesPagingFlow(likes, PagingConfig(pageSize = 10)) {
            ItemSavedLikesPagingSource(Order.LATEST, likes)
        }.cachedIn(backgroundScope)
        val main = StandardTestDispatcher(testScheduler)

        val firstVisit = LikesPresenter(main)
        val collecting = backgroundScope.launch { feed.collectLatest { firstVisit.collectFrom(it) } }
        runCurrent()
        assertEquals(listOf("first"), firstVisit.ids)
        collecting.cancel()
        runCurrent()

        Snapshot.withMutableSnapshot { likes.add(second) }
        runCurrent()

        val secondVisit = LikesPresenter(main)
        backgroundScope.launch { feed.collectLatest { secondVisit.collectFrom(it) } }
        runCurrent()
        assertEquals(listOf("second", "first"), secondVisit.ids)

        Snapshot.withMutableSnapshot { likes[1] = replacement }
        runCurrent()
        assertEquals(listOf("replacement", "first"), secondVisit.ids)

        Snapshot.withMutableSnapshot { likes.remove(first) }
        runCurrent()
        assertEquals(listOf("replacement"), secondVisit.ids)
    }

    private class LikesPresenter(main: CoroutineContext) : PagingDataPresenter<GifsInfo>(mainContext = main) {
        val ids: List<String> get() = snapshot().items.map { it.id }

        override suspend fun presentPagingDataEvent(event: PagingDataEvent<GifsInfo>) = Unit
    }
}
