package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.filter
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.lazy.viewportFractionCacheWindow
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.r.common.pagin.FeedPageNavigation
import com.client.xvideos.r.common.pagin.FeedPageQuery
import com.client.xvideos.r.common.pagin.ItemAccountSavedMediaPagingSource
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.common.pagin.ItemCollectionPagingSource
import com.client.xvideos.r.common.pagin.ItemEmptyPagingSource
import com.client.xvideos.r.common.pagin.ItemNailsPagingSource
import com.client.xvideos.r.common.pagin.ItemProfilePagingSource
import com.client.xvideos.r.common.pagin.ItemSavedLikesPagingSource
import com.client.xvideos.r.common.pagin.ItemSubscriptionsPagingSource
import com.client.xvideos.r.common.pagin.ItemTopPagingSource
import com.client.xvideos.r.common.pagin.savedLikesPagingFlow
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedList
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionItems
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Внутренний контейнер параметров фильтрации и пагинации ленты R.
 */
private data class SearchParams(
    val query: String,
    val sort: Order,
    val tags: Set<String>,
)

/**
 * Текст поиска раздела, от которого зависит лента [typePager]: по нему ищет
 * только TOP, остальным он не нужен.
 */
internal fun pagerSearchQuery(typePager: TypePager, searchText: String): String =
    if (typePager == TypePager.TOP) searchText.trim() else ""

/**
 * Скрывает ли лента заблокированные ролики: сетевые — да; лайки и коллекции
 * пользователь собрал сам, и блок-лист их не трогает.
 */
internal fun TypePager.appliesBlockList(): Boolean = when (this) {
    TypePager.TOP, TypePager.NICHES, TypePager.PROFILE, TypePager.SUBSCRIPTIONS -> true
    TypePager.R_SAVED_LIKES, TypePager.SAVED_COLLECTION, TypePager.ACCOUNT_SAVED_MEDIA, TypePager.EMPTY -> false
}

/**
 * Лента без роликов из [blockedIds]. Получатель — уже кэшированный поток: смена
 * блок-листа перефильтровывает загруженные страницы, источник заново не
 * запрашивается.
 *
 * Второй [cachedIn] после фильтра обязателен. `collectAsLazyPagingItems`
 * показывает загруженные страницы в первом кадре, только если поток — это
 * `SharedFlow` с кэшем страниц; `combine` и `filter` теряют и то и другое.
 * Без него сетка при каждом возврате на ленту собиралась с пустым списком и
 * сбрасывала прокрутку на начало.
 */
internal fun Flow<PagingData<GifsInfo>>.withoutBlocked(
    blockedIds: Flow<Set<String>>,
    scope: CoroutineScope,
): Flow<PagingData<GifsInfo>> =
    combine(blockedIds) { page, blocked ->
        if (blocked.isEmpty()) page else page.filter { it.id !in blocked }
    }.cachedIn(scope)

/**
 * Центральный стейт-холдер сетки медиаконтента R.
 *
 * Управляет:
 * - Потоком пагинации [pager] на базе AndroidX Paging 3;
 * - Позицией сетки [position] (само состояние сетки создаёт `LazyRow123`);
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
    val isCollection: Boolean = false,
    private val fixedPage: Int? = null,
    private val onFixedPageCount: (Int) -> Unit = {},
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

    /**
     * Позиция сетки (2-4 колонки). Хост живёт в ScreenModel, поэтому здесь только
     * позиция: само состояние сетки держало бы старую Activity, см.
     * [LazyGridPosition]. Состояние создаёт `LazyRow123`.
     */
    @OptIn(ExperimentalFoundationApi::class)
    val position = LazyGridPosition(cacheWindow = cacheWindow)

    /** Реактивный флаг наличия интернет-соединения. */
    val isConnected = connectivityObserver.isConnected.stateIn( scope, SharingStarted.WhileSubscribed(5000L), false )

    //////////////
    // StateFlow текущего типа сортировки
    private val _sortType = MutableStateFlow(startOrder) // или "popular", "oldest"
    /** Текущий порядок сортировки ленты. */
    val sortType: StateFlow<Order> = _sortType.asStateFlow()

    private val infiniteScrollSetting = if (fixedPage == null) {
        when (typePager) {
            TypePager.NICHES -> Settings.r_niche_infinite_scroll
            TypePager.PROFILE -> Settings.r_profile_infinite_scroll
            else -> null
        }
    } else null

    internal val pageNavigation = infiniteScrollSetting?.let {
        FeedPageNavigation(FeedPageQuery(startOrder, tags.value), it.field.value)
    }

    private data class NumberedPageKey(val query: FeedPageQuery, val index: Int)
    private data class NumberedPage(val host: LazyRow123Host, val job: Job)

    private val numberedPages = mutableMapOf<NumberedPageKey, NumberedPage>()

    internal fun setInfiniteScroll(enabled: Boolean) {
        infiniteScrollSetting?.setValue(enabled)
    }

    internal fun numberedPageHost(query: FeedPageQuery, index: Int): LazyRow123Host =
        numberedPages.getOrPut(NumberedPageKey(query, index)) {
            val job = SupervisorJob(scope.coroutineContext[Job])
            val host = LazyRow123Host(
                connectivityObserver = connectivityObserver,
                scope = CoroutineScope(scope.coroutineContext + job),
                typePager = typePager,
                extraString = extraString,
                startOrder = query.order,
                startColumns = columns,
                visibleProfileInfo = visibleProfileInfo,
                tags = MutableStateFlow(query.tags),
                block = block,
                redApi = redApi,
                savedRed = savedRed,
                downloadRed = downloadRed,
                search = search,
                fixedPage = index + 1,
                onFixedPageCount = { count ->
                    if (job.isActive) pageNavigation?.updatePageCount(query, infiniteScroll = false, count)
                },
            )
            NumberedPage(host, job)
        }.host

    internal fun onNumberedPageSettled(query: FeedPageQuery, index: Int) {
        val navigation = pageNavigation ?: return
        val state = navigation.state.value
        if (state.infiniteScroll || state.query != query) return
        navigation.selectPage(query, index)
        // Текущая и соседние сетки остаются доступны полноэкранному плееру.
        trimNumberedPages(query, index)
    }

    private fun trimNumberedPages(query: FeedPageQuery, index: Int) {
        numberedPages.entries.removeAll { (key, page) ->
            val outdated = key.query != query || key.index !in (index - 1)..(index + 1)
            if (outdated) page.job.cancel()
            outdated
        }
    }

    /** Сменяет сортировку ленты. */
    fun changeSortType(newSort: Order) {
        _sortType.value = newSort
        Timber.d("!!! *** Sort тип изменен в $newSort")
    }

    /**
     * Страницы ленты как пришли из источника. Pager пересоздаётся при смене
     * сортировки, тегов и — только у ленты TOP — текста поиска.
     *
     * Раньше в параметры каждой ленты входил текст обоих поисков раздела, хотя
     * ищет по нему одна TOP, а текст поиска ниш не читает ни один источник.
     * Подтверждение поиска пересоздавало все живые ленты: лайки, коллекции и
     * подписки открывались с начала.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val cachedPager: Flow<PagingData<GifsInfo>> =
        combine(search.searchTextDone, sortType, tags) { text, sort, tags ->
            SearchParams(pagerSearchQuery(typePager, text), sort, tags)
        }
            .onStart { if (typePager.appliesBlockList()) block.awaitInitialRefresh() }
            //.debounce(2000)                                       // ② ждём паузу ввода
            .distinctUntilChanged()                                 // ③ игнорируем дубли
            .flatMapLatest { params ->                              // ④ НОВЫЙ Pager при каждом изменении
                val pageQuery = FeedPageQuery(params.sort, params.tags)
                val shouldResetScroll = lastPagerParams?.let { it != params } == true
                lastPagerParams = params
                var resetScrollConsumed = false
                val config = PagingConfig(pageSize = 100, prefetchDistance = 10, initialLoadSize = 100)
                val pagingSourceFactory = {
                    Timber.d("!!! >>>pagingSourceFactory{...}")
                    if (shouldResetScroll && !resetScrollConsumed) {
                        resetScrollConsumed = true
                        gotoUp()
                    }
                    createPager(
                        typePager = typePager, sort = params.sort,
                        extraString = extraString, searchText = params.query,
                        tags = params.tags.toList(),
                        block = block, redApi = redApi, savedRed = savedRed,
                        fixedPage = fixedPage,
                        onPageCount = { count ->
                            if (fixedPage != null) {
                                onFixedPageCount(count)
                            } else {
                                pageNavigation?.updatePageCount(pageQuery, infiniteScroll = true, count)
                            }
                        },
                    )
                }
                when (typePager) {
                    TypePager.R_SAVED_LIKES -> savedLikesPagingFlow(savedRed.likes.list, config, pagingSourceFactory)
                    TypePager.ACCOUNT_SAVED_MEDIA -> {
                        val list = accountCollectionId?.let(::collectionItems) ?: AccountSavedKey(AccountSavedList.LIKES)
                        AccountSavedActions.revision(list)
                            .flatMapLatest { Pager(config, pagingSourceFactory = pagingSourceFactory).flow }
                    }
                    else -> Pager(config, pagingSourceFactory = pagingSourceFactory).flow
                }
            }
            .cachedIn(scope)

    /**
     * Поток пагинированных данных [PagingData] элементов [GifsInfo] без
     * заблокированных роликов.
     *
     * Источники сверяются с блок-листом только при загрузке страницы, поэтому
     * ролик, заблокированный позже, оставался в открытой ленте до ручного
     * обновления, а лента подписок блок-лист не применяла вовсе. Фильтр стоит
     * после [cachedIn]: блокировка не перезагружает ленту, см. [withoutBlocked].
     */
    val pager: Flow<PagingData<GifsInfo>> =
        if (typePager.appliesBlockList()) cachedPager.withoutBlocked(block.blockedIds, scope) else cachedPager

    internal val isAccountLikes: Boolean
        get() = typePager == TypePager.ACCOUNT_SAVED_MEDIA && extraString.isEmpty()

    internal val accountCollectionId: String?
        get() = extraString.takeIf { typePager == TypePager.ACCOUNT_SAVED_MEDIA && it.isNotEmpty() }


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
    /** Сбой обновления, о котором уже сообщили: по одному сообщению на сбой. */
    var announcedRefreshFailure: Throwable? = null
    private var lastPagerParams: SearchParams? = null

    /** Прокрутить сетку в самый верх; без экрана — показать её с начала. */
    fun gotoUp() { scope.launch { position.scrollToItem(0) } }

    init {
        RFeedSessionStore.register(this)
        if (infiniteScrollSetting != null && pageNavigation != null) {
            scope.launch { infiniteScrollSetting.field.collect(pageNavigation::setInfiniteScroll) }
            scope.launch {
                combine(sortType, tags, ::FeedPageQuery).collect { query ->
                    pageNavigation.updateQuery(query)
                    trimNumberedPages(query, pageNavigation.state.value.pageIndex)
                }
            }
        }
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
    fixedPage: Int? = null,
    onPageCount: (Int) -> Unit = {},
): PagingSource<Int, GifsInfo> {
    val pagingSourceFactory = when (typePager) {

        TypePager.NICHES -> ItemNailsPagingSource(
            order = sort,
            nichesName = extraString,
            block = block,
            redApi = redApi,
            fixedPage = fixedPage,
            onPageCount = onPageCount,
        )
        TypePager.TOP -> { ItemTopPagingSource( sort = sort, searchText = searchText, block = block, redApi = redApi ) }
        TypePager.R_SAVED_LIKES -> { ItemSavedLikesPagingSource(sort, savedRed.likes.list) }
        TypePager.SUBSCRIPTIONS -> { ItemSubscriptionsPagingSource(savedRed) }

        TypePager.PROFILE -> ItemProfilePagingSource(
            profileName = extraString,
            sort = sort,
            block = block,
            redApi = redApi,
            tags = tags,
            fixedPage = fixedPage,
            onPageCount = onPageCount,
        )
        TypePager.EMPTY -> { ItemEmptyPagingSource() }
        TypePager.SAVED_COLLECTION -> { ItemCollectionPagingSource( collection = extraString, savedRed = savedRed ) }
        TypePager.ACCOUNT_SAVED_MEDIA -> ItemAccountSavedMediaPagingSource(redApi, extraString.takeIf { it.isNotEmpty() })

    }
    return pagingSourceFactory
}
