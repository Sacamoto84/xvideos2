package com.client.xvideos.xlive.ui.feed

import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.network.ROOM_PAGE_SIZE
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

/**
 * Бесконечная лента комнат: страницы по [ROOM_PAGE_SIZE] со сдвигом. Список на
 * сайте живой — пока листаешь, комнаты меняют место, поэтому страницы
 * склеиваются без повторов по нику.
 *
 * Отбор ленты (пол, подборка, тег) знает тот, кто её держит: [loadPage] берёт
 * его сам на момент запроса. Держит ленту ScreenModel — корень раздела или
 * экран тега, — и [scope] у неё его.
 *
 * @param loadPage страница со сдвигом под текущий отбор.
 * @param label отбор для журнала — без ников.
 */
class XLiveRoomsFeed(
    private val scope: CoroutineScope,
    private val loadPage: suspend (offset: Int) -> XLiveRoomPage,
    private val label: () -> String,
) {

    private val _state = MutableStateFlow(XLiveFeedState())
    val state: StateFlow<XLiveFeedState> = _state.asStateFlow()

    /**
     * Позиция сетки. Живёт у ленты, а не в композиции: корневой навигатор не
     * хранит состояние экрана, с которого ушли, и после комнаты сетка вставала
     * бы в начало.
     */
    val gridPosition = LazyGridPosition()

    private var loadJob: Job? = null
    private var nextOffset = 0

    /** Лента с нуля: экран загрузки, первая страница, сетка в начало. Смена отбора и «Повторить». */
    fun reload() {
        _state.value = XLiveFeedState(isLoading = true)
        scope.launch { gridPosition.scrollToItem(0) }
        loadFirstPage()
    }

    /** Свайп вниз: старые комнаты на месте, пока не пришла свежая первая страница. */
    fun refresh() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isRefreshing = true, error = null, loadMoreError = null) }
        loadFirstPage()
    }

    /** Нажатие на открытую подборку: сетка в начало, дальше как [refresh]. */
    fun refreshFromTop() {
        if (_state.value.isLoading) return
        scope.launch { gridPosition.scrollToItem(0) }
        refresh()
    }

    /**
     * Следующая страница, когда сетка докрутилась до конца. Повторные вызовы во
     * время загрузки не в счёт, как и вызовы без первой страницы.
     */
    fun loadMore() {
        val feed = _state.value
        val busy = feed.isLoading || feed.isRefreshing || feed.isLoadingMore
        val blocked = feed.rooms.isEmpty() || feed.isEndReached || feed.loadMoreError != null
        if (busy || blocked) return
        _state.update { it.copy(isLoadingMore = true) }
        val offset = nextOffset
        loadJob = scope.launch {
            try {
                val page = loadPage(offset)
                logPage(offset, page)
                nextOffset = offset + ROOM_PAGE_SIZE
                _state.update { current ->
                    current.copy(
                        rooms = (current.rooms + page.rooms).distinctBy { it.username }.toImmutableList(),
                        isLoadingMore = false,
                        isEndReached = isLast(page),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: страница эфиров не получена — %s", e.javaClass.simpleName)
                _state.update { it.copy(isLoadingMore = false, loadMoreError = "Не удалось загрузить ещё") }
            }
        }
    }

    fun retryLoadMore() {
        _state.update { it.copy(loadMoreError = null) }
        loadMore()
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val page = loadPage(0)
                logPage(0, page)
                nextOffset = ROOM_PAGE_SIZE
                _state.value = XLiveFeedState(
                    rooms = page.rooms.distinctBy { it.username }.toImmutableList(),
                    isEndReached = isLast(page),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: эфиры не получены — %s", e.javaClass.simpleName)
                val keepRooms = _state.value.isRefreshing && _state.value.rooms.isNotEmpty()
                if (keepRooms) {
                    _state.update { it.copy(isRefreshing = false) }
                    EventBus.postEvent(Event.ShowSnackBar(UiMessage.Error("Не удалось обновить эфиры")))
                } else {
                    _state.value = XLiveFeedState(error = "Не удалось загрузить эфиры")
                }
            }
        }
    }

    private fun logPage(offset: Int, page: XLiveRoomPage) {
        Timber.i("XLive: эфиры %s, сдвиг %d — комнат %d из %d", label(), offset, page.rooms.size, page.total)
    }

    /** Последняя страница: в ней нет обычных комнат (промо сайт кладёт на каждую) или сдвиг дошёл до конца. */
    private fun isLast(page: XLiveRoomPage): Boolean =
        page.rooms.none { !it.isPromoted } || nextOffset >= page.total
}
