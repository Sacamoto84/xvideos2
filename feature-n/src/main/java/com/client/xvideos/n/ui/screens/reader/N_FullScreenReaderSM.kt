package com.client.xvideos.n.ui.screens.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel для полноэкранного просмотрщика (ридера) манги.
 * Отвечает за проверку и переключение статуса лайка страниц и добавление в коллекции.
 */
@Stable
class N_FullScreenReaderSM @Inject constructor(
    val saved: NSaved
) : ScreenModel {

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)

    fun isPageLiked(galleryId: Long, pageNumber: Int): Boolean =
        saved.likes.isLiked(galleryId, pageNumber)

    fun toggleLikePage(gallery: NGallery, pageIndex: Int): Boolean {
        val page = gallery.pages.getOrNull(pageIndex) ?: return false
        val picture = NPicture.fromPage(gallery.id, gallery.mediaId, gallery.title, page)
        return saved.likes.toggleLike(picture)
    }

    fun addToCollection(gallery: NGallery, pageIndex: Int, collectionName: String): Boolean {
        val page = gallery.pages.getOrNull(pageIndex) ?: return false
        val picture = NPicture.fromPage(gallery.id, gallery.mediaId, gallery.title, page)
        return saved.collection.addToCollection(collectionName, picture)
    }

    fun createCollectionAndAdd(gallery: NGallery, pageIndex: Int, collectionName: String): Boolean {
        saved.collection.createCollection(collectionName)
        return addToCollection(gallery, pageIndex, collectionName)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_FullScreenReaderModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_FullScreenReaderSM::class)
    abstract fun bindN_FullScreenReaderSM(sm: N_FullScreenReaderSM): ScreenModel
}
