package com.client.xvideos.n.ui.screens.gallery

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.repository.CloudflareBlockedException
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.saved.NFavoritesRepository
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScreenModel экрана деталей манги.
 */
@Stable
class ScreenNGallerySM @AssistedInject constructor(
    @Assisted val galleryId: Long,
    private val repository: NRepository,
    private val favoritesRepository: NFavoritesRepository,
    val saved: NSaved
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(galleryId: Long): ScreenNGallerySM
    }

    private val _uiState = MutableStateFlow<NGalleryUiState>(NGalleryUiState.Loading)
    val uiState: StateFlow<NGalleryUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(favoritesRepository.isFavorite(galleryId))
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    val columns = Settings.n_galleriesTab_column_current_count.field.value

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)
    var targetPictureForCollection by mutableStateOf<NPicture?>(null)

    fun openAddToCollection(picture: NPicture) {
        targetPictureForCollection = picture
        isSelectCollectionOpen = true
    }

    fun addToCollection(collectionName: String) {
        val picture = targetPictureForCollection ?: return
        saved.collection.addToCollection(collectionName, picture)
        isSelectCollectionOpen = false
        targetPictureForCollection = null
    }

    fun createCollectionAndAdd(collectionName: String) {
        saved.collection.createCollection(collectionName)
        addToCollection(collectionName)
        isCreateCollectionOpen = false
    }

    fun isPageLiked(pageNumber: Int): Boolean =
        saved.likes.isLiked(galleryId, pageNumber)

    fun toggleLikePage(gallery: NGallery, page: NPage): Boolean {
        val picture = NPicture.fromPage(gallery.id, gallery.mediaId, gallery.title, page)
        return saved.likes.toggleLike(picture)
    }

    init {
        loadGallery()
    }

    fun toggleFavorite() {
        val gallery = (_uiState.value as? NGalleryUiState.Success)?.gallery ?: return
        _isFavorite.value = favoritesRepository.toggleFavorite(gallery)
    }

    fun loadGallery() {
        screenModelScope.launch {
            _uiState.value = NGalleryUiState.Loading
            repository.getGalleryDetails(galleryId).fold(
                onSuccess = { gallery ->
                    _uiState.value = NGalleryUiState.Success(gallery)
                },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryUiState.CloudflareRequired(error.url)
                    } else {
                        _uiState.value = NGalleryUiState.Error(error.localizedMessage ?: "Ошибка загрузки")
                    }
                }
            )
        }
    }

    /**
     * Запрашивает ID случайной манги и передает его в колбэк навигации.
     */
    fun openRandomGallery(onSuccess: (Long) -> Unit) {
        screenModelScope.launch {
            repository.getRandomGalleryId().fold(
                onSuccess = { id -> onSuccess(id) },
                onFailure = { error ->
                    if (error is CloudflareBlockedException) {
                        _uiState.value = NGalleryUiState.CloudflareRequired(error.url)
                    }
                }
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleNGallery {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenNGallerySM.Factory::class)
    abstract fun bindScreenNGallerySMFactory(
        factory: ScreenNGallerySM.Factory
    ): ScreenModelFactory
}

sealed interface NGalleryUiState {
    data object Loading : NGalleryUiState
    data class Success(val gallery: NGallery) : NGalleryUiState
    data class Error(val message: String) : NGalleryUiState
    data class CloudflareRequired(val url: String) : NGalleryUiState
}
