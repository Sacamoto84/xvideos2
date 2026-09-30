package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.model.GifsInfo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Stable
class ScreenSavedDownloadSM @Inject constructor(
    val downloadRed: DownloadRed
) : ScreenModel {
    fun delete(item: GifsInfo) {
        downloadRed.delete(item)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedDownload {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedDownloadSM::class)
    abstract fun bindScreenRedSavedDownloadScreenModel(hiltListScreenModel: ScreenSavedDownloadSM): ScreenModel
}
