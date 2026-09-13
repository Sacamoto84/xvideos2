package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.lazy.grid.LazyGridState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    val gridState = LazyGridState()

    /**
     * Переименование коллекции на пуле IO.
     */
    fun renameCollection(oldName: String, newName: String) {
        savedL.scope.launch(Dispatchers.IO) {
            savedL.collection.renameCollection(oldName, newName)
        }
    }

    /**
     * Удаление коллекции на пуле IO.
     */
    fun deleteCollection(name: String) {
        savedL.scope.launch(Dispatchers.IO) {
            savedL.collection.deleteCollection(name)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollection {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenLSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}

