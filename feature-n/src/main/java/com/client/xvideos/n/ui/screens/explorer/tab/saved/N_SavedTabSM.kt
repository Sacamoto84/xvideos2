package com.client.xvideos.n.ui.screens.explorer.tab.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.saved.NSaved
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel вкладки сохранённых закладок манги (координирует лайки, мангу и коллекции).
 */
@Stable
class N_SavedTabSM @Inject constructor(
    val saved: NSaved
) : ScreenModel {

    var screenType by mutableIntStateOf(0)

    val favorites: SnapshotStateList<NGalleryPreview> = saved.favorites.favorites

    val columns = Settings.n_saved_column_current_count.field

    fun removeFavorite(id: Long) {
        saved.favorites.removeFavorite(id)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class N_SavedTabModule {
    @Binds
    @IntoMap
    @ScreenModelKey(N_SavedTabSM::class)
    abstract fun bindN_SavedTabSM(sm: N_SavedTabSM): ScreenModel
}
