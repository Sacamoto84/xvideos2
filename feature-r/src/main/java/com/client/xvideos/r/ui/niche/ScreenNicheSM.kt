package com.client.xvideos.r.ui.niche

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.r.model.NichesInfo
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
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
import timber.log.Timber

/**
 * [ScreenModel] экрана отдельной ниши (категории) в RedGifs.
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
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
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
            search = search,
            searchNiches = searchNiches
        )

    init {
        Timber.d("ScreenNicheSM init")

        lazyHost.columns = 2

        if (cleanNicheName.isNotBlank()) {
            // getOrThrow бросает при любом отказе сети, и раньше это закрывало
            // приложение. Экран остаётся пустым, но остаётся.
            screenModelScope.launchCatching(message = "Ниша $cleanNicheName не загрузилась") {
                niche = redApi.getNiche(cleanNicheName).getOrThrow().niche            // Нужно кешировать
                related = redApi.getNichesRelated(cleanNicheName).getOrThrow()      // Нужно кешировать
                topCreator = redApi.getNichesTopCreators(cleanNicheName).getOrThrow()  // Нужно кешировать
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
