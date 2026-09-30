package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.AlbumDetails
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber
import javax.inject.Inject

@Stable
class ScreenLSavedAlbumsSM @Inject constructor(
    val saved: SavedL
) : ScreenModel {

    val state = LazyGridState()

    val albums: List<AlbumDetails>
        get() = saved.albums.list.filter { it.id.toLongOrNull() != null }

    init {
        Timber.d("ScreenLSavedAlbumsSM init")
        if (albums.isEmpty()) saved.albums.refresh()
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLSavedAlbumsSM onDispose")
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedAlbums {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLSavedAlbumsSM::class)
    abstract fun bindScreenRedFulScreenSreenModel(hiltListScreenModel: ScreenLSavedAlbumsSM): ScreenModel
}
