package com.client.xvideos.xlive.ui.root

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.snackbar.UiMessage
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.model.XLiveRoomPage
import com.client.xvideos.xlive.network.ROOM_PAGE_SIZE
import com.client.xvideos.xlive.network.XLiveApi
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

/**
 * Корень раздела XLive: нижняя вкладка, пол, подборка и лента «Эфиры».
 *
 * Лента бесконечная: страницы по [ROOM_PAGE_SIZE] со сдвигом. Список на сайте
 * живой — пока листаешь, комнаты меняют место, поэтому страницы склеиваются
 * без повторов по нику.
 *
 * @param loadPage источник страницы списка; подменяется в тестах.
 */
class XLiveSM(
    private val loadPage: suspend (XLiveGender, XLiveCollection, Int) -> XLiveRoomPage = XLiveApi::rooms,
) : ScreenModel {

    private val _tab = MutableStateFlow(XLiveTab.ROOMS)
    val tab: StateFlow<XLiveTab> = _tab.asStateFlow()

    private val _gender = MutableStateFlow(XLiveGender.ALL)
    val gender: StateFlow<XLiveGender> = _gender.asStateFlow()

    private val _collection = MutableStateFlow(XLiveCollection.POPULAR)
    val collection: StateFlow<XLiveCollection> = _collection.asStateFlow()

    private val _feed = MutableStateFlow(XLiveFeedState())
    val feed: StateFlow<XLiveFeedState> = _feed.asStateFlow()

    /**
     * Позиция сетки. Живёт здесь, а не в композиции: корневой навигатор не хранит
     * состояние экрана, с которого ушли, и после комнаты сетка вставала бы в начало.
     */
    val gridPosition = LazyGridPosition()

    private var loadJob: Job? = null
    private var nextOffset = 0

    init {
        reload()
    }

    fun selectTab(tab: XLiveTab) {
        _tab.value = tab
    }

    fun selectGender(gender: XLiveGender) {
        if (_gender.value == gender) return
        _gender.value = gender
        reload()
    }

    fun selectCollection(collection: XLiveCollection) {
        if (_collection.value == collection) return
        _collection.value = collection
        reload()
    }

    /** Лента с нуля: экран загрузки, первая страница, сетка в начало. Смена отбора и «Повторить». */
    fun reload() {
        _feed.value = XLiveFeedState(isLoading = true)
        screenModelScope.launch { gridPosition.scrollToItem(0) }
        loadFirstPage()
    }

    /** Свайп вниз: старые комнаты на месте, пока не пришла свежая первая страница. */
    fun refresh() {
        if (_feed.value.isLoading) return
        _feed.update { it.copy(isRefreshing = true, error = null, loadMoreError = null) }
        loadFirstPage()
    }

    /**
     * Следующая страница, когда сетка докрутилась до конца. Повторные вызовы во
     * время загрузки не в счёт, как и вызовы без первой страницы.
     */
    fun loadMore() {
        val feed = _feed.value
        val busy = feed.isLoading || feed.isRefreshing || feed.isLoadingMore
        val blocked = feed.rooms.isEmpty() || feed.isEndReached || feed.loadMoreError != null
        if (busy || blocked) return
        _feed.update { it.copy(isLoadingMore = true) }
        val gender = _gender.value
        val collection = _collection.value
        val offset = nextOffset
        loadJob = screenModelScope.launch {
            try {
                val page = loadPage(gender, collection, offset)
                logPage(gender, collection, offset, page)
                nextOffset = offset + ROOM_PAGE_SIZE
                _feed.update { current ->
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
                _feed.update { it.copy(isLoadingMore = false, loadMoreError = "Не удалось загрузить ещё") }
            }
        }
    }

    fun retryLoadMore() {
        _feed.update { it.copy(loadMoreError = null) }
        loadMore()
    }

    private fun loadFirstPage() {
        loadJob?.cancel()
        val gender = _gender.value
        val collection = _collection.value
        loadJob = screenModelScope.launch {
            try {
                val page = loadPage(gender, collection, 0)
                logPage(gender, collection, 0, page)
                nextOffset = ROOM_PAGE_SIZE
                _feed.value = XLiveFeedState(
                    rooms = page.rooms.distinctBy { it.username }.toImmutableList(),
                    isEndReached = isLast(page),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("XLive: эфиры не получены — %s", e.javaClass.simpleName)
                val keepRooms = _feed.value.isRefreshing && _feed.value.rooms.isNotEmpty()
                if (keepRooms) {
                    _feed.update { it.copy(isRefreshing = false) }
                    EventBus.postEvent(Event.ShowSnackBar(UiMessage.Error("Не удалось обновить эфиры")))
                } else {
                    _feed.value = XLiveFeedState(error = "Не удалось загрузить эфиры")
                }
            }
        }
    }

    private fun logPage(gender: XLiveGender, collection: XLiveCollection, offset: Int, page: XLiveRoomPage) {
        Timber.i("XLive: эфиры %s/%s, сдвиг %d — комнат %d из %d", gender, collection, offset, page.rooms.size, page.total)
    }

    /** Последняя страница: в ней нет обычных комнат (промо сайт кладёт на каждую) или сдвиг дошёл до конца. */
    private fun isLast(page: XLiveRoomPage): Boolean =
        page.rooms.none { !it.isPromoted } || nextOffset >= page.total
}
