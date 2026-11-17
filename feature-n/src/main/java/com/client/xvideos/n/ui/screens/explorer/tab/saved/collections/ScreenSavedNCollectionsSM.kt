package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

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

    val currentCollectionItems: SnapshotStateList<NPicture> = saved.collection.currentCollectionItems

    val columns = Settings.n_collectionTab_column_current_count.field

    var isCreateDialogOpen by mutableStateOf(false)
    var isSelectOtherCollectionOpen by mutableStateOf(false)
    var isCreateOtherCollectionOpen by mutableStateOf(false)
    var targetPictureForOtherCollection by mutableStateOf<NPicture?>(null)

    fun openAddToCollection(picture: NPicture) {
        targetPictureForOtherCollection = picture
        isSelectOtherCollectionOpen = true
    }

    fun addTargetToCollection(collectionName: String) {
        val picture = targetPictureForOtherCollection ?: return
        saved.collection.addToCollection(collectionName, picture)
        isSelectOtherCollectionOpen = false
        targetPictureForOtherCollection = null
    }

    fun createCollectionAndAddTarget(collectionName: String) {
        saved.collection.createCollection(collectionName)
        addTargetToCollection(collectionName)
        isCreateOtherCollectionOpen = false
    }

    fun isLiked(picture: NPicture): Boolean =
        saved.likes.isLiked(picture.id)

    fun toggleLike(picture: NPicture): Boolean =
        saved.likes.toggleLike(picture)

    fun openCollection(name: String) {
        saved.collection.openCollection(name)
    }

    fun closeCollection() {
        saved.collection.closeCollection()
    }

    fun createCollection(name: String) {
        saved.collection.createCollection(name)
    }

    fun deleteCollection(name: String) {
        saved.collection.deleteCollection(name)
    }

    fun removeItem(picture: NPicture) {
        val current = currentCollectionName ?: return
        saved.collection.removeFromCollection(current, picture.id)
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
