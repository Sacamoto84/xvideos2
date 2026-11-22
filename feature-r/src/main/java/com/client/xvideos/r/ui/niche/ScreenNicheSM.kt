package com.client.xvideos.r.ui.niche

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import dagger.Binds
import dagger.Module
import androidx.compose.runtime.Stable
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * [ScreenModel] экрана отдельной ниши (категории) в R.
 *
 * Инкапсулирует:
 * - Загрузку метаданных ниши [niche], похожих ниш [related] и топовых авторов [topCreator];
 * - Управление хостом пагинации и сетки контента [lazyHost] ([LazyRow123Host]).
 *
 * @param nicheName Название ниши (передается через Assisted Injection).
 */
@Stable
class ScreenNicheSM @AssistedInject constructor(
    @Assisted val nicheName: String,
    private val connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    /** Фабрика Assisted Injection для создания [ScreenNicheSM] с параметром [nicheName]. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(nicheName: String): ScreenNicheSM
    }

    /** Очищенное от пробелов имя ниши. */
    val cleanNicheName = nicheName.trim()

    /** Метаданные текущей ниши. */
    var niche: NichesInfo by mutableStateOf(NichesInfo())
    /** Список похожих ниш. */
    var related by mutableStateOf(NichesResponse(emptyList(), 0, 0, 0))
    /** Топовые авторы данной ниши. */
    var topCreator by mutableStateOf(TopCreatorsResponse(emptyList()))

    /** Хост сетки видеороликов ниши с поддержкой смены колонок и пагинации. */
    val lazyHost =
        LazyRow123Host(
            connectivityObserver = connectivityObserver, scope = screenModelScope,
            extraString = cleanNicheName,
            typePager = TypePager.NICHES,
            block = block,
            redApi = redApi,
            savedRed = savedRed,
            downloadRed = downloadRed,
            search = search
        )

    private data class PageKey(val order: Order, val index: Int)
    private data class PageHost(val host: LazyRow123Host, val job: Job)

    private val numberedPages = mutableMapOf<PageKey, PageHost>()

    internal fun numberedPageHost(order: Order, index: Int): LazyRow123Host =
        numberedPages.getOrPut(PageKey(order, index)) {
            val job = SupervisorJob(screenModelScope.coroutineContext[Job])
            val host = LazyRow123Host(
                connectivityObserver = connectivityObserver,
                scope = CoroutineScope(screenModelScope.coroutineContext + job),
                typePager = TypePager.NICHES,
                extraString = cleanNicheName,
                startOrder = order,
                startColumns = lazyHost.columns,
                block = block,
                redApi = redApi,
                savedRed = savedRed,
                downloadRed = downloadRed,
                search = search,
                fixedNichePage = index + 1,
                onFixedNichePageCount = { count ->
                    if (job.isActive && lazyHost.sortType.value == order) {
                        lazyHost.nichePageNavigation?.updatePageCount(infiniteScroll = false, count = count)
                    }
                },
            )
            PageHost(host, job)
        }.host

    internal fun onNumberedPageSettled(order: Order, index: Int) {
        if (order != lazyHost.sortType.value) return
        lazyHost.nichePageNavigation?.selectPage(index)
        // Хост нужен и полноэкранному плееру. Храним текущую и соседние страницы
        // до следующего перелистывания, даже когда сетка ушла из композиции.
        val iterator = numberedPages.iterator()
        while (iterator.hasNext()) {
            val (key, page) = iterator.next()
            if (key.order != order || key.index !in (index - 1)..(index + 1)) {
                page.job.cancel()
                iterator.remove()
            }
        }
    }

    init {
        Timber.d("ScreenNicheSM init")

        lazyHost.columns = 2

        if (cleanNicheName.isNotBlank()) {
            // Три независимых запроса идут разом, и сбой одного не отменяет
            // остальные. Раньше они шли подряд в одном блоке: первый упал —
            // второй и третий не выполнялись, а о сбое не узнавал никто.
            screenModelScope.launch {
                val failures = listOf(
                    async { runCatchingCancellable { niche = redApi.getNiche(cleanNicheName).getOrThrow().niche } },
                    async { runCatchingCancellable { related = redApi.getNichesRelated(cleanNicheName).getOrThrow() } },
                    async { runCatchingCancellable { topCreator = redApi.getNichesTopCreators(cleanNicheName).getOrThrow() } },
                ).awaitAll().mapNotNull { it.exceptionOrNull() }

                failures.forEach { Timber.w("ScreenNicheSM: часть данных ниши не загрузилась: ${it.javaClass.simpleName}") }
                failures.firstOrNull()?.let {
                    SnackBar.error("Ниша загрузилась не полностью: ${it.toRUserMessage()}")
                }
            }
        } else {
            Timber.w("ScreenNicheSM init: пустое имя ниши")
        }
    }
}

/** Hilt-модуль привязки Assisted-фабрики экрана ниши. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedNiche {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNicheSM.Factory::class)
    abstract fun bindHiltNicheScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenNicheSM.Factory
    ): ScreenModelFactory

}
