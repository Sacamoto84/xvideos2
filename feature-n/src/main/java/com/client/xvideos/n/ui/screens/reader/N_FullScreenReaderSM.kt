package com.client.xvideos.n.ui.screens.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.repository.NRepository
import com.client.xvideos.n.saved.NSaved
import com.client.xvideos.n.ui.screens.gallery.NGalleryUiState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ScreenModel для полноэкранного просмотрщика (ридера) манги.
 * Отвечает за проверку и переключение статуса лайка страниц и добавление в коллекции.
 */
@Stable
class N_FullScreenReaderSM @Inject constructor(
    val saved: NSaved,
    private val repository: NRepository
) : ScreenModel {

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)

    internal suspend fun loadGallery(
        galleryId: Long,
        albumState: StateFlow<NGalleryUiState>?,
        forceRefresh: Boolean
    ): Result<NGallery> {
        if (albumState == null || forceRefresh) return repository.getGalleryDetails(galleryId, forceRefresh)
        return when (val state = albumState.first { it !is NGalleryUiState.Loading }) {
            is NGalleryUiState.Success -> Result.success(state.gallery)
            is NGalleryUiState.Error -> Result.failure(IllegalStateException(state.message))
            is NGalleryUiState.CloudflareRequired -> Result.failure(state.challenge)
            NGalleryUiState.Loading -> error("Загрузка альбома не завершена")
        }
    }

    fun isPageLiked(galleryId: Long, pageNumber: Int): Boolean? =
        saved.likes.isLiked(galleryId, pageNumber)

    fun setLiked(picture: NPicture, liked: Boolean) {
        screenModelScope.launch { saved.likes.setLiked(picture, liked) }
    }

    fun addToCollection(gallery: NGallery, pageIndex: Int, collectionName: String, create: Boolean = false) {
        val page = gallery.pages.getOrNull(pageIndex) ?: return
        val picture = NPicture.fromPage(gallery.id, gallery.mediaId, gallery.title, page)
        screenModelScope.launch {
            if (create) saved.collection.createCollectionAndAdd(collectionName, picture)
            else saved.collection.addToCollection(collectionName, picture)
        }
    }

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? =
        saved.collection.getCollectionCoverUrl(collectionName, items)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_FullScreenReaderModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_FullScreenReaderSM::class)
    abstract fun bindN_FullScreenReaderSM(sm: N_FullScreenReaderSM): ScreenModel
}
