package com.client.xvideos.screenSettings

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Stable
class AppSettingsSM @Inject constructor(
    val savedRed: SavedRed,
    val blockRed: BlockRed,
    val downloadRed: DownloadRed,
    val savedL: SavedL
) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsModule {
    @Binds
    @IntoMap
    @ScreenModelKey(AppSettingsSM::class)
    abstract fun bindAppSettingsSM(sm: AppSettingsSM): ScreenModel
}
