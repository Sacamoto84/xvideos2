package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

/**
 * Какой сбой загрузки список R показывает и о каком сообщает сразу.
 *
 * Проверено на устройстве: после неудачного жеста обновления Paging не
 * запрашивает следующие страницы, пока обновление не повторят. Список при этом
 * оставался как был — без сообщения и без кнопки: лента молча обрывалась на
 * уже загруженном, а погасший индикатор выглядел как «готово».
 */
class RListLoadErrorsTest {

    private val failure = IOException("нет сети")
    private val idle = LoadState.NotLoading(endOfPaginationReached = false)

    private fun states(refresh: LoadState = idle, append: LoadState = idle) = CombinedLoadStates(
        refresh = refresh,
        prepend = idle,
        append = append,
        source = LoadStates(refresh = refresh, prepend = idle, append = append),
    )

    @Test
    fun `сбой обновления показывается строкой в конце списка`() {
        assertSame(failure, listLoadError(states(refresh = LoadState.Error(failure))))
    }

    @Test
    fun `сбой следующей страницы показывается строкой в конце списка`() {
        assertSame(failure, listLoadError(states(append = LoadState.Error(failure))))
    }

    @Test
    fun `без сбоев строки ошибки нет`() {
        assertNull(listLoadError(states()))
        assertNull(listLoadError(states(refresh = LoadState.Loading, append = LoadState.Loading)))
    }

    @Test
    fun `о сбое обновления непустого списка сообщают сразу`() {
        assertSame(failure, refreshFailureToAnnounce(LoadState.Error(failure), itemCount = 100, announced = null))
    }

    @Test
    fun `об одном и том же сбое сообщают один раз`() {
        // Состояние загрузки переживает экран: при возврате на него и после
        // пересоздания активности тот же сбой приходит снова.
        assertNull(refreshFailureToAnnounce(LoadState.Error(failure), itemCount = 100, announced = failure))
    }

    @Test
    fun `о новом сбое после прежнего сообщают снова`() {
        val next = IOException("снова нет сети")

        assertSame(next, refreshFailureToAnnounce(LoadState.Error(next), itemCount = 100, announced = failure))
    }

    @Test
    fun `у пустого списка сбой обновления показывает только строка`() {
        assertNull(refreshFailureToAnnounce(LoadState.Error(failure), itemCount = 0, announced = null))
    }

    @Test
    fun `пока обновление идёт или прошло, сообщать не о чем`() {
        assertNull(refreshFailureToAnnounce(LoadState.Loading, itemCount = 100, announced = null))
        assertNull(refreshFailureToAnnounce(idle, itemCount = 100, announced = null))
    }
}
