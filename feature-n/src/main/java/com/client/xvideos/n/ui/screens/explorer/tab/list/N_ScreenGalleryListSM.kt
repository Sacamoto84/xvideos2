package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.repository.NRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel вкладки со списком свежей манги с постраничной пагинацией.
 */
@Stable
class N_ScreenGalleryListSM @Inject constructor(
    private val repository: NRepository
) : ScreenModel {

    private val _sortType = MutableStateFlow(NSortType.RECENT)
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

    /**
     * Загружает ID случайной манги и вызывает [onSuccess].
     */
    fun openRandomGallery(onSuccess: (Long) -> Unit) {
        screenModelScope.launch {
            repository.getRandomGalleryId().onSuccess(onSuccess)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNGalleryList {
    @Binds
    @IntoMap
    @ScreenModelKey(N_ScreenGalleryListSM::class)
    abstract fun bindN_ScreenGalleryListSM(model: N_ScreenGalleryListSM): ScreenModel
}
