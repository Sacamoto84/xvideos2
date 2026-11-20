package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.repository.CloudflareBlockedException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ScreenModel вкладки серверного избранного манги.
 */
@Stable
class ScreenNServerFavoritesSM @Inject constructor(
    private val repository: NRepository,
    private val favoritesRepository: NFavoritesRepository
) : ScreenModel {

    val pages = NGalleryPages(screenModelScope, fetch = repository::getFavoritesPage)

    private class PendingRemoval(val galleryId: Long, val apiKey: String)
    private val pendingRemovals = ArrayDeque<PendingRemoval>()
    private var processingRemovals = false
    private val _removalChallenge = MutableStateFlow<CloudflareBlockedException?>(null)
    internal val removalChallenge = _removalChallenge.asStateFlow()

    init {
        screenModelScope.launch {
            combine(Settings.n_api_key.field, Settings.n_session_cookie.field) { key, session -> key to session }
                .drop(1)
                .collect { (key, _) ->
                    pages.reset()
                    if (_removalChallenge.value != null && pendingRemovals.firstOrNull()?.apiKey != key) {
                        _removalChallenge.value = null
                        processRemovals()
                    }
                }
        }
        screenModelScope.launch {
            repository.serverFavoriteRevision.drop(1).collect { pages.refresh() }
        }
    }

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val columns = Settings.n_saved_column_current_count.field
    val apiKey = Settings.n_api_key.field

    /**
     * Импортирует все элементы текущей страницы серверного избранного в локальное избранное.
     */
    fun importCurrentPage() {
        val items = (pages[pages.currentIndex] as? NGalleryListUiState.Success)?.items ?: return
        if (items.isEmpty() || _isImporting.value) return

        screenModelScope.launch {
            _isImporting.value = true
            try {
                val result = withContext(Dispatchers.IO) { favoritesRepository.importFavorites(items) }
                when {
                    result.failed > 0 -> SnackBar.error(
                        "Импортировано: ${result.added}. Уже сохранено: ${result.skipped}. Ошибок: ${result.failed}"
                    )
                    result.added > 0 -> SnackBar.success("Импортировано ${result.added} элементов в сохранённое")
                    else -> SnackBar.info("Все элементы страницы уже сохранены локально")
                }
            } finally {
                _isImporting.value = false
            }
        }
    }

    /**
     * Импортирует выбранную работу в локальное избранное.
     */
    fun importSingle(preview: NGalleryPreview) {
        screenModelScope.launch {
            withContext(Dispatchers.IO) { favoritesRepository.importFavorite(preview) }.fold(
                onSuccess = { added ->
                    if (added) SnackBar.success("Добавлено в локальное избранное")
                    else SnackBar.info("Уже сохранено локально")
                },
                onFailure = { SnackBar.error("Не удалось добавить в локальное избранное") }
            )
        }
    }

    /**
     * Проверяет, сохранена ли галерея локально.
     */
    internal val localFavorites = favoritesRepository.favorites

    /**
     * Удаляет галерею из серверного избранного и перезагружает текущую страницу.
     */
    fun removeServerFavorite(galleryId: Long, apiKey: String) {
        if (apiKey.isBlank() || apiKey != NAuthState.currentApiKey) return
        if (pendingRemovals.any { it.galleryId == galleryId && it.apiKey == apiKey }) return
        pendingRemovals.addLast(PendingRemoval(galleryId, apiKey))
        processRemovals()
    }

    private fun processRemovals() {
        if (processingRemovals || _removalChallenge.value != null || pendingRemovals.isEmpty()) return
        processingRemovals = true
        screenModelScope.launch {
            try {
                while (pendingRemovals.isNotEmpty()) {
                    val request = pendingRemovals.first()
                    if (NAuthState.currentApiKey != request.apiKey) {
                        pendingRemovals.removeFirst()
                        continue
                    }
                    val result = repository.setServerFavorite(request.galleryId, false, request.apiKey)
                    val failure = result.exceptionOrNull()
                    if (NAuthState.currentApiKey == request.apiKey && failure is CloudflareBlockedException) {
                        _removalChallenge.value = failure
                        break
                    }
                    pendingRemovals.removeFirst()
                    if (NAuthState.currentApiKey != request.apiKey) continue
                    if (failure == null) SnackBar.success("Удалено из избранного на сервере")
                    else SnackBar.error("Не удалось удалить с сервера: ${failure.localizedMessage ?: "ошибка"}")
                }
            } finally {
                processingRemovals = false
            }
        }
    }

    internal fun finishRemovalChallenge(verified: Boolean) {
        if (_removalChallenge.value == null) return
        _removalChallenge.value = null
        if (!verified) pendingRemovals.removeFirstOrNull()
        processRemovals()
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
