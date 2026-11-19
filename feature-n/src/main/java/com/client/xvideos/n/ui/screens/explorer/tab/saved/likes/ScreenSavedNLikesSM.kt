package com.client.xvideos.n.ui.screens.explorer.tab.saved.likes

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import com.client.xvideos.n.ui.screens.picture.savedPictureIndex
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * ScreenModel вкладки лайков страниц манги nhentai.
 */
@Stable
class ScreenSavedNLikesSM @Inject constructor(
    private val saved: NSaved
) : ScreenModel {

    val likes: SnapshotStateList<NPicture> = saved.likes.likes
    val picturesPosition = LazyGridPosition()

    internal fun showPicture(pictureId: String, previousIndex: Int) {
        screenModelScope.launch {
            savedPictureIndex(likes, pictureId, previousIndex)?.let { picturesPosition.scrollToItem(it) }
        }
    }

    val collections: SnapshotStateList<CollectionEntity<NPicture>> = saved.collection.collections

    val columns = Settings.n_likesTab_column_current_count.field

    var isSelectCollectionOpen by mutableStateOf(false)
    var isCreateCollectionOpen by mutableStateOf(false)
    var targetPictureForCollection by mutableStateOf<NPicture?>(null)

    fun openAddToCollection(picture: NPicture) {
        targetPictureForCollection = picture
        isSelectCollectionOpen = true
    }

    fun addToCollection(collectionName: String, create: Boolean = false) {
        val picture = targetPictureForCollection ?: return
        screenModelScope.launch {
            if (create) saved.collection.createCollectionAndAdd(collectionName, picture)
            else saved.collection.addToCollection(collectionName, picture)
        }
        isSelectCollectionOpen = false
        isCreateCollectionOpen = false
        targetPictureForCollection = null
    }

    fun removeLike(picture: NPicture) {
        screenModelScope.launch { saved.likes.removeLike(picture.id) }
    }

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? =
        saved.collection.getCollectionCoverUrl(collectionName, items)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenSavedNLikesModule {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNLikesSM::class)
    abstract fun bindScreenSavedNLikesSM(sm: ScreenSavedNLikesSM): ScreenModel
}
