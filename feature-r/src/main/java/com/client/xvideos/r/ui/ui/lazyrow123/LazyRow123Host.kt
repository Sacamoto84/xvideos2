package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.pagin.ItemCollectionPagingSource
import com.client.xvideos.r.common.pagin.ItemEmptyPagingSource
import com.client.xvideos.r.common.pagin.ItemNailsPagingSource
import com.client.xvideos.r.common.pagin.ItemProfilePagingSource
import com.client.xvideos.r.common.pagin.ItemSavedLikesPagingSource
import com.client.xvideos.r.common.pagin.ItemSubscriptionsPagingSource
import com.client.xvideos.r.common.pagin.ItemTopPagingSource
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

private data class SearchParams(
    val query: String,
    val sort: Order,
    val tags: String,
    val queryNiches: String
)

@Stable
@OptIn(FlowPreview::class)
class LazyRow123Host(
    val connectivityObserver: ConnectivityObserver,
    val scope: CoroutineScope,
    val typePager: TypePager,
    val extraString: String = "",
    val startOrder: Order = Order.LATEST,
    val startColumns: Int = 2,
    val visibleProfileInfo: Boolean = true,
    val tags: StateFlow<Set<String>> = MutableStateFlow(emptySet()),
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
    val isCollection: Boolean = false
) {
    companion object {
        private val nextFeedId = AtomicInteger(0)

        fun normalizeColumns(value: Int): Int {
            return value.takeIf { it in 2..4 } ?: 2
        }
    }

    val feedKey: String = "RFeed:${typePager.name}:${extraString}:${nextFeedId.incrementAndGet()}"

    /**
     * Окно предзагрузки сетки. Доля вьюпорта, а не фиксированные dp: число
     * колонок здесь переключаемое (2..4), и одна и та же константа в dp
     * означала бы разное количество рядов.
     *
     * Заднее окно ненулевое намеренно: элемент сетки — живое видео-превью со
     * своим `ExoPlayer`, и при `behind = 0` прокрутка вверх пересоздавала
     * плееры, которые только что были отпущены.
     */
    @OptIn(ExperimentalFoundationApi::class)
    val cacheWindow = viewportFractionCacheWindow(ahead = 0.5f, behind = 0.15f)

    @OptIn(ExperimentalFoundationApi::class)
    val state: LazyGridState = LazyGridState(cacheWindow = cacheWindow)

    val stateColumn = LazyListState()

    val isConnected = connectivityObserver.isConnected.stateIn( scope, SharingStarted.WhileSubscribed(5000L), false )

    //////////////
    // StateFlow текущего типа сортировки
    private val _sortType = MutableStateFlow(startOrder) // или "popular", "oldest"
    val sortType: StateFlow<Order> = _sortType.asStateFlow()

    fun changeSortType(newSort: Order) {
        _sortType.value = newSort
        Timber.d("!!! *** Sort тип изменен в $newSort")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pager: Flow<PagingData<GifsInfo>> =
        combine( search.searchTextDone, sortType, tags, searchNiches.searchTextDone )
        { text, sort, tags, textNiches ->
            SearchParams(text.trim(), sort, tags.joinToString(","), textNiches)
        }
            //.debounce(2000)                                       // ② ждём паузу ввода
            .distinctUntilChanged()                                 // ③ игнорируем дубли
            .flatMapLatest { params ->                              // ④ НОВЫЙ Pager при каждом изменении
                val shouldResetScroll = lastPagerParams?.let { it != params } == true
                lastPagerParams = params
                var resetScrollConsumed = false
                Pager(
                    config = PagingConfig( pageSize = 100, prefetchDistance = 10, initialLoadSize = 100 ),
                    pagingSourceFactory = {
                        Timber.d("!!! >>>pagingSourceFactory{...}")
                        if (shouldResetScroll && !resetScrollConsumed) {
                            resetScrollConsumed = true
                            gotoUp()
                            gotoUpColumn()
                        }
                        createPager(
                            typePager = typePager,  sort = params.sort,
                            extraString = extraString, searchText = params.query,
                            tags = tags.value.toList(),
                            block = block, redApi = redApi, savedRed = savedRed, searchNiches = searchNiches,
                            textNiches = params.queryNiches
                        )
                    }
                ).flow
            }
            .cachedIn(scope)


    private var _columns by mutableIntStateOf(normalizeColumns(startColumns))
    var columns: Int
        get() = _columns
        set(value) {
            _columns = normalizeColumns(value)
        }

    var currentIndex by mutableIntStateOf(0)
    var currentIndexGoto by mutableIntStateOf(0)
    var returnToIndex by mutableIntStateOf(-1)
    private var lastPagerParams: SearchParams? = null

    fun gotoUp() { scope.launch { state.scrollToItem(0) } }

    fun gotoUpColumn() { scope.launch { stateColumn.scrollToItem(0) } }

    init {
        RFeedSessionStore.register(this)
    }

}

fun createPager(
    typePager: TypePager,
    sort: Order,
    extraString: String,
    searchText: String,
    tags: List<String> = emptyList(),
    block: BlockRed,
    redApi: RedApi,
    savedRed: SavedRed,
    searchNiches: R_SearchNiches,
    textNiches: String,
): PagingSource<Int, GifsInfo> {
    val pagingSourceFactory = when (typePager) {

        TypePager.NICHES -> { ItemNailsPagingSource( order = sort, nichesName = extraString, block = block, redApi = redApi ) }
        TypePager.TOP -> { ItemTopPagingSource( sort = sort, searchText = searchText, block = block, redApi = redApi ) }
        TypePager.R_SAVED_LIKES -> { ItemSavedLikesPagingSource(sort, savedRed) }
        TypePager.SUBSCRIPTIONS -> { ItemSubscriptionsPagingSource(savedRed) }

        TypePager.PROFILE -> { ItemProfilePagingSource( profileName = extraString, sort = sort, block = block, redApi = redApi, tags = tags ) }
        TypePager.EMPTY -> { ItemEmptyPagingSource() }
        TypePager.SAVED_COLLECTION -> { ItemCollectionPagingSource( collection = extraString, savedRed = savedRed ) }

    }
    return pagingSourceFactory
}
