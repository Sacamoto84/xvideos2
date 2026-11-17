package com.client.xvideos.n.ui.screens.picture

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel полноэкранного просмотрщика сохранённых картинок.
 */
@Stable
class N_FullScreenPictureSM @Inject constructor(
    val saved: NSaved
) : ScreenModel {

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)

    fun isPictureLiked(picture: NPicture): Boolean =
        saved.likes.isLiked(picture.id)

    fun toggleLike(picture: NPicture): Boolean =
        saved.likes.toggleLike(picture)

    fun addToCollection(picture: NPicture, collectionName: String): Boolean =
        saved.collection.addToCollection(collectionName, picture)

    fun createCollectionAndAdd(picture: NPicture, collectionName: String): Boolean {
        saved.collection.createCollection(collectionName)
        return addToCollection(picture, collectionName)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_FullScreenPictureModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_FullScreenPictureSM::class)
    abstract fun bindN_FullScreenPictureSM(sm: N_FullScreenPictureSM): ScreenModel
}
