package com.client.xvideos.r.ui.explorer.tab.niches

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.explorer.RNavigationState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * [ScreenModel] вкладки каталога ниш в разделе Explorer RedGifs.
 *
 * Управляет порядком сортировки ниш [sortType], связывает его с навигационным состоянием [navigationState].
 *
 * @param navigationState Глобальное навигационное состояние раздела RedGifs.
 * @param savedRed Фасад локальных данных (включая кэш ниш).
 * @param search Стейт-холдер поиска ниш.
 */
@Stable
class ScreenRedExplorerNichesSM @Inject constructor(
    val navigationState: RNavigationState,
    val savedRed: SavedRed,
    val search: R_SearchNiches,
) : ScreenModel {

    private val _sortType = MutableStateFlow(navigationState.nichesSort)
    /** Текущий тип сортировки каталога ниш. */
    val sortType = _sortType.asStateFlow()

    /** Изменяет тип сортировки и сохраняет его в навигационном состоянии. */
    fun changeSortType(order: Order) {
        navigationState.updateNichesSort(order)
        _sortType.value = order
    }
}

/** Hilt-модуль привязки [ScreenRedExplorerNichesSM]. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerNichesSM::class)
    abstract fun bindScreenRedExplorerNichesSreenModel(hiltListScreenModel: ScreenRedExplorerNichesSM): ScreenModel
}
