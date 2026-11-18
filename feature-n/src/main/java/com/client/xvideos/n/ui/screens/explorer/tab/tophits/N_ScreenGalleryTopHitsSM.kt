package com.client.xvideos.n.ui.screens.explorer.tab.tophits

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryPages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ScreenModel вкладки популярной манги (Top Hits) с постраничной пагинацией.
 */
@Stable
class N_ScreenGalleryTopHitsSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _sortType = MutableStateFlow(NSortType.POPULAR_ALL_TIME)
    val sortType = _sortType.asStateFlow()
    val columns = Settings.n_galleriesTab_column_current_count.field

    val pages = NGalleryPages(screenModelScope) { number ->
        repository.getGalleriesPage(page = number, sort = _sortType.value)
    }

    fun setSortType(sort: NSortType) {
        if (_sortType.value == sort) return
        _sortType.value = sort
        pages.reset()
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNGalleryTopHits {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGalleryTopHitsSM::class)
    abstract fun bindN_ScreenGalleryTopHitsSM(model: N_ScreenGalleryTopHitsSM): ScreenModel
}
