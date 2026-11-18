package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.saved.NFavoritesRepository
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel вкладки серверного избранного манги.
 */
@Stable
class ScreenNServerFavoritesSM @Inject constructor(
    private val repository: NRepository,
    private val favoritesRepository: NFavoritesRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow<NGalleryListUiState>(NGalleryListUiState.Loading)
    val uiState: StateFlow<NGalleryListUiState> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val columns = Settings.n_saved_column_current_count.field

    private var loadJob: Job? = null

    init {
        loadPage(1)
    }

    fun loadPage(page: Int) {
        _currentPage.value = page
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            _uiState.value = NGalleryListUiState.Loading
            repository.getFavoritesPage(page).fold(
                onSuccess = { result ->
                    _currentPage.value = result.page
                    _totalPages.value = result.totalPages.coerceAtLeast(1)
                    _uiState.value = NGalleryListUiState.Success(result.items)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryListUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryListUiState.Error(
                            error.localizedMessage ?: "Ошибка загрузки избранного с сервера"
                        )
                    }
                }
            )
        }
    }

    /**
     * Импортирует все элементы текущей страницы серверного избранного в локальное избранное.
     */
    fun importCurrentPage() {
        val items = (_uiState.value as? NGalleryListUiState.Success)?.items ?: return
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
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNServerFavorites {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenNServerFavoritesSM::class)
    abstract fun bindScreenNServerFavoritesSM(model: ScreenNServerFavoritesSM): ScreenModel
}
