package com.client.xvideos.x.screens.dashboards.vm

import androidx.compose.foundation.pager.PagerState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.launch
import javax.inject.Inject

class ScreenXDashBoardsScreenModel @Inject constructor(
    val saved : SavedX
) : ScreenModel {
    /** Количество колонок true-2 false-1 */
    val countRow = Settings.xvideos_row2
    val pagerState: PagerState = PagerState(0) { 20000 }
    fun openVideoPlayer(url: String, navigator: Navigator) { navigator.push(ScreenX_VideoPlayer(url)) }

    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }
    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id) // O(1) по множеству id

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleDashBoards {

    @Binds
    @IntoMap
    @ScreenModelKey(ScreenXDashBoardsScreenModel::class)
    abstract fun bindScreenDashBoardsScreenModel(hiltListScreenModel: ScreenXDashBoardsScreenModel): ScreenModel

}