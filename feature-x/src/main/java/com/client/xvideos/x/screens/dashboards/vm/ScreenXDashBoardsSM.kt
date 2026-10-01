package com.client.xvideos.x.screens.dashboards.vm

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.x.feature.country.CountryState
import com.client.xvideos.x.feature.net.readHtmlFromURLWebView
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.parcer.parseSiteCountryFlag
import com.client.xvideos.x.parcer.parserListVideo
import com.client.xvideos.x.screens.common.PageCache
import com.client.xvideos.x.screens.dashboards.buildDashboardUrl
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber
import javax.inject.Inject

/**
 * ScreenModel главного экрана дашбордов (лент и категорий) раздела X.
 *
 * Управляет состоянием вкладок, пагинатором, открытием плеера и действиями над избранным/загрузками.
 *
 * @property saved Фасад локальных данных раздела X ([SavedX]).
 */
@Stable
class ScreenXDashBoardsScreenModel @Inject constructor(
    val saved : SavedX
) : ScreenModel {

    /** Состояние горизонтального пейджера страниц выдачи. */
    val pagerState: PagerState = PagerState(0) { 20000 }

    /** Текущий главный таб нижней панели: 0 — Dashboards, 1 — Savable (сохранённое). */
    var mainTab by mutableIntStateOf(0)

    /** Текущий под-таб раздела Savable: 0 — Favorites (пока единственный). */
    var savedTab by mutableIntStateOf(0)

    /**
     * Открывает экран видеоплеера по карточке ролика.
     *
     * @param item Карточка ролика [ItemsX].
     * @param navigator Навигатор Voyager.
     */
    fun openVideoPlayer(item: ItemsX, navigator: Navigator) {
        navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item))
    }

    /** Скачать (сохранить) видео в раздел «Сохранённое». */
    fun download(item: ItemsX) = saved.downloads.download(item)

    /** Сохранить видео целиком в общую галерею (Movies/xvideos_download). */
    fun saveToGallery(item: ItemsX) = saved.downloads.saveToGallery(item)

    /** Добавить ролик в избранное. */
    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }

    /** Удалить ролик из избранного. */
    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }

    /** Быстрая O(1) проверка нахождения в избранном. */
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id) // O(1) по множеству id

    /**
     * Загруженные страницы дашборда. ScreenModel живёт всю сессию, поэтому страница
     * устаревает через [DASHBOARD_PAGE_TTL_MS], а выбор другой страны сбрасывает кэш.
     */
    private val dashboardPages = PageCache<ImmutableList<ItemsX>>(
        maxPages = MAX_CACHED_PAGES,
        ttlMs = DASHBOARD_PAGE_TTL_MS,
    )
    private var cachedCountryEpoch = CountryState.userSelectionEpoch

    /** Уже загруженная страница [index] или `null` — страницу надо грузить. */
    fun cachedDashboardPage(index: Int): ImmutableList<ItemsX>? {
        dropPagesOfOtherCountry()
        return dashboardPages[index]
    }

    /**
     * Страница [index] дашборда: из памяти, иначе из сети. Пустой список — страница
     * не загрузилась или не разобралась.
     */
    suspend fun loadDashboardPage(index: Int): ImmutableList<ItemsX> {
        cachedDashboardPage(index)?.let { return it }
        val epoch = CountryState.userSelectionEpoch
        val (flag, items) = fetchDashboardPage(index)
        flag?.let { CountryState.updateCurrent(it) }
        val page = items.toImmutableList()
        // Пока шла загрузка, могли выбрать другую страну — такую страницу не кэшируем.
        if (page.isNotEmpty() && epoch == CountryState.userSelectionEpoch) {
            dropPagesOfOtherCountry()
            dashboardPages[index] = page
        }
        return page
    }

    private fun dropPagesOfOtherCountry() {
        if (cachedCountryEpoch != CountryState.userSelectionEpoch) {
            dashboardPages.clear()
            cachedCountryEpoch = CountryState.userSelectionEpoch
        }
    }

    private suspend fun fetchDashboardPage(index: Int): Pair<String?, List<ItemsX>> {
        Timber.d("Дашборд: загрузка страницы %d", index)
        val html = readHtmlFromURLWebView(buildDashboardUrl(index))
        return withContext(Dispatchers.Default) {
            val document = Jsoup.parse(html)
            val flag = parseSiteCountryFlag(document)
            val items = parserListVideo(document)
                .filter { !it.href.contains("THUMBNUM") }
                .distinctBy { it.id }
            flag to items
        }
    }

    private companion object {
        const val MAX_CACHED_PAGES = 20
        const val DASHBOARD_PAGE_TTL_MS = 10 * 60 * 1000L
    }
}

/**
 * Hilt-модуль привязки [ScreenXDashBoardsScreenModel] в многопользовательскую карту ScreenModel.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleDashBoards {

    @Binds
    @IntoMap
    @ScreenModelKey(ScreenXDashBoardsScreenModel::class)
    abstract fun bindScreenDashBoardsScreenModel(hiltListScreenModel: ScreenXDashBoardsScreenModel): ScreenModel

}
