package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.saved.NFavoritesRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryPages
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel вкладки серверного избранного манги.
 */
@Stable
class ScreenNServerFavoritesSM @Inject constructor(
    private val repository: NRepository,
    private val favoritesRepository: NFavoritesRepository
) : ScreenModel {

    val pages = NGalleryPages(screenModelScope, fetch = repository::getFavoritesPage)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val columns = Settings.n_saved_column_current_count.field

    /**
     * Импортирует все элементы текущей страницы серверного избранного в локальное избранное.
     */
    fun importCurrentPage() {
        val items = (pages[pages.currentIndex] as? NGalleryListUiState.Success)?.items ?: return
        if (items.isEmpty() || _isImporting.value) return

        screenModelScope.launch {
            _isImporting.value = true
            val count = favoritesRepository.importFavorites(items)
            _isImporting.value = false
            if (count > 0) {
                SnackBar.success("Импортировано $count элементов в сохранённое")
            } else {
                SnackBar.info("Все элементы страницы уже сохранены локально")
            }
        }
    }

    /**
     * Импортирует выбранную работу в локальное избранное.
     */
    fun importSingle(preview: NGalleryPreview) {
        val added = favoritesRepository.importFavorite(preview)
        if (added) {
            SnackBar.success("Добавлено в локальное избранное")
        } else {
            SnackBar.info("Уже сохранено локально")
        }
    }

    /**
     * Проверяет, сохранена ли галерея локально.
     */
    fun isLocallySaved(galleryId: Long): Boolean = favoritesRepository.isFavorite(galleryId)

    /**
     * Удаляет галерею из серверного избранного и перезагружает текущую страницу.
     */
    fun removeServerFavorite(galleryId: Long) {
        screenModelScope.launch {
            repository.toggleServerFavorite(galleryId).fold(
                onSuccess = {
                    SnackBar.success("Удалено из избранного на сервере")
                    pages.refresh()
                },
                onFailure = { error ->
                    SnackBar.error("Не удалось удалить с сервера: ${error.localizedMessage ?: "ошибка"}")
                }
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNServerFavorites {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenNServerFavoritesSM::class)
    abstract fun bindScreenNServerFavoritesSM(model: ScreenNServerFavoritesSM): ScreenModel
}
