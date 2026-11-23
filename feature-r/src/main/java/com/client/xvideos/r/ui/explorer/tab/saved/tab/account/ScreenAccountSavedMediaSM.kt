package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Stable
class ScreenAccountSavedMediaSM @AssistedInject constructor(
    @Assisted collectionId: String,
    connectivityObserver: ConnectivityObserver,
    block: BlockRed,
    redApi: RedApi,
    savedRed: SavedRed,
    downloadRed: DownloadRed,
    search: R_SearchExplorer,
) : ScreenModel {
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(collectionId: String): ScreenAccountSavedMediaSM
    }

    internal val state = accountSavedContent { accountScope ->
        LazyRow123Host(
            connectivityObserver = connectivityObserver,
            scope = accountScope,
            typePager = TypePager.ACCOUNT_SAVED_MEDIA,
            extraString = collectionId,
            block = block,
            redApi = redApi,
            savedRed = savedRed,
            downloadRed = downloadRed,
            search = search,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedAccountSavedMedia {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenAccountSavedMediaSM.Factory::class)
    abstract fun bindScreenModel(factory: ScreenAccountSavedMediaSM.Factory): ScreenModelFactory
}
