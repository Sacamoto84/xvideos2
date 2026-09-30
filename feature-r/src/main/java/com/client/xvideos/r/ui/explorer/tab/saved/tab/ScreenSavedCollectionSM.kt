package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
class ScreenSavedCollectionSM @Inject constructor(
    val block: BlockRed,
    val savedRed: SavedRed,
) : ScreenModel {

    val gridState = LazyGridState()

    /**
     * Переименование коллекции на пуле IO.
     */
    fun renameCollection(oldName: String, newName: String) {
        savedRed.scope.launch(Dispatchers.IO) {
            savedRed.collections.renameCollection(oldName, newName)
        }
    }

    /**
     * Рекурсивное удаление коллекции на пуле IO.
     */
    fun deleteCollection(name: String) {
        savedRed.scope.launch(Dispatchers.IO) {
            savedRed.collections.deleteCollection(name)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedCollection {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenRedSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}
