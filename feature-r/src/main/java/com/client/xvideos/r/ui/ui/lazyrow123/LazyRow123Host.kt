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

/**
 * Внутренний контейнер параметров фильтрации и пагинации ленты RedGifs.
 */
private data class SearchParams(
    val query: String,
    val sort: Order,
    val tags: String,
    val queryNiches: String
)

/**
 * Центральный стейт-холдер сетки медиаконтента RedGifs.
 *
 * Управляет:
 * - Потоком пагинации [pager] на базе AndroidX Paging 3;
 * - Состоянием скролла сетки [state] ([LazyGridState]) и одиночной колонки [stateColumn] ([LazyListState]);
 * - Размером окна предзагрузки [cacheWindow] (ahead 0.5f, behind 0.15f) для гладкой работы пула видеоплееров;
 * - Динамическим числом колонок [columns] (от 2 до 4);
 * - Реакцией на изменение поискового запроса, сортировки и выбранных тегов с автосбросом скролла вверх;
 * - Регистрацией в глобальном [RFeedSessionStore] для бесшовного перехода в полноэкранный плеер.
 *
 * @property connectivityObserver Монитор сетевого подключения.
 * @property scope Скоп для кэширования пагинации.
 * @property typePager Тип ленты (топ, ниши, лайки, подписки, профиль, коллекции).
 * @property extraString Дополнительный строковый аргумент (имя профиля, ниши или коллекции).
 * @property startOrder Начальный порядок сортировки.
 * @property startColumns Начальное число колонок сетки (2..4).
 * @property visibleProfileInfo Показывать ли плашку автора над элементами.
 * @property tags Реактивный поток выбранных тегов.
 */
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

        /** Нормализует число колонок сетки, удерживая его в диапазоне 2..4 (по умолчанию 2). */
        fun normalizeColumns(value: Int): Int {
            return value.takeIf { it in 2..4 } ?: 2
        }
    }

    /** Уникальный строковый ключ ленты в [RFeedSessionStore]. */
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

    /** Состояние скролла для сеточного режима (2-4 колонки). */
    @OptIn(ExperimentalFoundationApi::class)
    val state: LazyGridState = LazyGridState(cacheWindow = cacheWindow)

    /** Состояние скролла для одноколоночного режима. */
    val stateColumn = LazyListState()

    /** Реактивный флаг наличия интернет-соединения. */
    val isConnected = connectivityObserver.isConnected.stateIn( scope, SharingStarted.WhileSubscribed(5000L), false )

    //////////////
    // StateFlow текущего типа сортировки
    private val _sortType = MutableStateFlow(startOrder) // или "popular", "oldest"
    /** Текущий порядок сортировки ленты. */
    val sortType: StateFlow<Order> = _sortType.asStateFlow()

    /** Сменяет сортировку ленты. */
    fun changeSortType(newSort: Order) {
        _sortType.value = newSort
        Timber.d("!!! *** Sort тип изменен в $newSort")
    }

    /**
     * Поток пагинированных данных [PagingData] элементов [GifsInfo].
     * Пересоздает Pager при смене поискового текста, сортировки или тегов.
     */
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
    /** Количество колонок сетки (2..4). */
    var columns: Int
        get() = _columns
        set(value) {
            _columns = normalizeColumns(value)
        }

    /** Текущий просматриваемый индекс ролика. */
    var currentIndex by mutableIntStateOf(0)
    /** Целевой индекс для программного скролла. */
    var currentIndexGoto by mutableIntStateOf(0)
    /** Индекс возврата при выходе из полноэкранного режима. */
    var returnToIndex by mutableIntStateOf(-1)
    private var lastPagerParams: SearchParams? = null

    /** Прокрутить сетку в самый верх. */
    fun gotoUp() { scope.launch { state.scrollToItem(0) } }

    /** Прокрутить одиночную колонку в самый верх. */
    fun gotoUpColumn() { scope.launch { stateColumn.scrollToItem(0) } }

    init {
        RFeedSessionStore.register(this)
    }

}

/**
 * Фабричная функция создания экземпляра [PagingSource] для соответствующего типа ленты [TypePager].
 */
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
