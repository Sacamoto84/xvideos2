package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections

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
 * ScreenModel вкладки коллекций страниц манги.
 */
@Stable
class ScreenSavedNCollectionsSM @Inject constructor(
    val saved: NSaved
) : ScreenModel {

    val collections: SnapshotStateList<CollectionEntity<NPicture>> = saved.collection.collections

    val currentCollectionName: String?
        get() = saved.collection.currentCollectionName

    val currentCollectionItems: List<NPicture>
        get() = saved.collection.currentCollectionItems

    val picturesPosition = LazyGridPosition()

    internal fun showPicture(collectionName: String, pictureId: String, previousIndex: Int) {
        screenModelScope.launch {
            if (currentCollectionName != collectionName) saved.collection.openCollection(collectionName)
            if (currentCollectionName == collectionName) {
                val index = savedPictureIndex(currentCollectionItems, pictureId, previousIndex)
                if (index != null) picturesPosition.scrollToItem(index)
            }
        }
    }

    val columns = Settings.n_collectionTab_column_current_count.field

    var isCreateDialogOpen by mutableStateOf(false)
    var isSelectOtherCollectionOpen by mutableStateOf(false)
    var isCreateOtherCollectionOpen by mutableStateOf(false)
    var targetPictureForOtherCollection by mutableStateOf<NPicture?>(null)

    fun openAddToCollection(picture: NPicture) {
        targetPictureForOtherCollection = picture
        isSelectOtherCollectionOpen = true
    }

    fun addTargetToCollection(collectionName: String, create: Boolean = false) {
        val picture = targetPictureForOtherCollection ?: return
        screenModelScope.launch {
            if (create) saved.collection.createCollectionAndAdd(collectionName, picture)
            else saved.collection.addToCollection(collectionName, picture)
        }
        isSelectOtherCollectionOpen = false
        isCreateOtherCollectionOpen = false
        targetPictureForOtherCollection = null
    }

    fun isLiked(picture: NPicture): Boolean =
        saved.likes.isLiked(picture.id)

    fun toggleLike(picture: NPicture) {
        screenModelScope.launch { saved.likes.toggleLike(picture) }
    }

    fun setCover(picture: NPicture) {
        val current = currentCollectionName ?: return
        screenModelScope.launch { saved.collection.setCollectionCover(current, picture) }
    }

    fun getCollectionCoverUrl(collectionName: String, items: List<NPicture>): String? =
        saved.collection.getCollectionCoverUrl(collectionName, items)

    fun openCollection(name: String) {
        screenModelScope.launch {
            if (currentCollectionName != name) picturesPosition.scrollToItem(0)
            saved.collection.openCollection(name)
        }
    }

    fun closeCollection() {
        screenModelScope.launch { saved.collection.closeCollection() }
    }

    fun createCollection(name: String) {
        screenModelScope.launch { saved.collection.createCollection(name) }
    }

    fun renameCollection(oldName: String, newName: String) {
        screenModelScope.launch { saved.collection.renameCollection(oldName, newName) }
    }

    fun deleteCollection(name: String) {
        screenModelScope.launch { saved.collection.deleteCollection(name) }
    }

    fun removeItem(picture: NPicture) {
        val current = currentCollectionName ?: return
        screenModelScope.launch { saved.collection.removeFromCollection(current, picture.id) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenSavedNCollectionsModule {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedNCollectionsSM::class)
    abstract fun bindScreenSavedNCollectionsSM(sm: ScreenSavedNCollectionsSM): ScreenModel
}
