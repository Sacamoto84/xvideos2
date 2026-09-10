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
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Состояние вкладки ниш и его привязка к Hilt.
 *
 * Выделено из `R_ScreenNichesTab.kt` (было 526 строк). Тело не менялось —
 * перенос дословный.
 */
class ScreenRedExplorerNichesSM @Inject constructor(
    val navigationState: RNavigationState,
    val savedRed: SavedRed,
    val search: R_SearchNiches,
) : ScreenModel {

    private val _sortType = MutableStateFlow(navigationState.nichesSort)
    val sortType = _sortType.asStateFlow()

    fun changeSortType(order: Order) {
        navigationState.updateNichesSort(order)
        _sortType.value = order
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerNichesSM::class)
    abstract fun bindScreenRedExplorerNichesSreenModel(hiltListScreenModel: ScreenRedExplorerNichesSM): ScreenModel
}
