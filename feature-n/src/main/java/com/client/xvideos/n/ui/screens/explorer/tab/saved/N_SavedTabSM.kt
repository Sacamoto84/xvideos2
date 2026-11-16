package com.client.xvideos.n.ui.screens.explorer.tab.saved

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.saved.NFavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel вкладки сохранённых закладок манги.
 */
@Stable
class N_SavedTabSM @Inject constructor(
    private val favoritesRepository: NFavoritesRepository
) : ScreenModel {

    val favorites: SnapshotStateList<NGalleryPreview> = favoritesRepository.favorites

    val columns = Settings.n_galleriesTab_column_current_count.field

    fun removeFavorite(id: Long) {
        favoritesRepository.removeFavorite(id)
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
