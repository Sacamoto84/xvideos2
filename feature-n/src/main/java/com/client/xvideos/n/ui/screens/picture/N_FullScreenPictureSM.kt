package com.client.xvideos.n.ui.screens.picture

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.ui.scroll.PagerPosition
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * ScreenModel полноэкранного просмотрщика сохранённых картинок.
 */
@Stable
class N_FullScreenPictureSM @Inject constructor(
    val saved: NSaved
) : ScreenModel {

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)
    private var targetPictureForCollection: NPicture? = null

    private var loadedPages: NSavedPicturePages? = null

    fun isPictureLiked(picture: NPicture): Boolean? =
        saved.likes.isLiked(picture.id)

    fun setLiked(picture: NPicture, liked: Boolean) {
        screenModelScope.launch { saved.likes.setLiked(picture, liked) }
    }

    internal suspend fun loadPictures(
        collectionName: String?,
        initialPicture: NPicture,
        selectedPictureId: String
    ): NSavedPicturePages {
        loadedPages?.let { return it }
        val items = if (collectionName == null) {
            saved.likes.load()
            saved.likes.likes.toList()
        } else {
            saved.collection.readPictures(collectionName)
        }
        val pictures = if (items.any { it.id == initialPicture.id || it.id == selectedPictureId }) {
            items
        } else listOf(initialPicture) + items
        val initialIndex = pictures.indexOfFirst { it.id == initialPicture.id }.coerceAtLeast(0)
        val index = savedPictureIndex(pictures, selectedPictureId, initialIndex) ?: 0
        return NSavedPicturePages(pictures, PagerPosition(index)).also { loadedPages = it }
    }

    fun openCollectionPicker(picture: NPicture) {
        targetPictureForCollection = picture
        isSelectCollectionOpen = true
    }

    fun dismissCollectionPicker() {
        isSelectCollectionOpen = false
        isCreateCollectionOpen = false
        targetPictureForCollection = null
    }

    fun addToCollection(collectionName: String, create: Boolean = false) {
        val picture = targetPictureForCollection ?: return
        dismissCollectionPicker()
        screenModelScope.launch {
            if (create) saved.collection.createCollectionAndAdd(collectionName, picture)
            else saved.collection.addToCollection(collectionName, picture)
        }
    }

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? =
        saved.collection.getCollectionCoverUrl(collectionName, items)
}

internal data class NSavedPicturePages(val pictures: List<NPicture>, val position: PagerPosition)

internal fun savedPictureIndex(pictures: List<NPicture>, pictureId: String, previousIndex: Int): Int? {
    if (pictures.isEmpty()) return null
    val matchingIndex = pictures.indexOfFirst { it.id == pictureId }
    return if (matchingIndex >= 0) matchingIndex else previousIndex.coerceIn(pictures.indices)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_FullScreenPictureModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_FullScreenPictureSM::class)
    abstract fun bindN_FullScreenPictureSM(sm: N_FullScreenPictureSM): ScreenModel
}
