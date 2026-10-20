package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.ui.scroll.LazyGridPosition
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

    /** Позиция сетки коллекций; состояние сетки создаёт экран, см. [LazyGridPosition]. */
    val gridPosition = LazyGridPosition()

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
