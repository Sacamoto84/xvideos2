package com.client.xvideos.screenSettings

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.LLoginFormState
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.screenSettings.backup.BackupController
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
    val savedL: SavedL,
    val backup: BackupController,
) : ScreenModel {
    /** Форма входа в L, которую настройки показывают по кнопке «Войти». */
    val lLoginForm = LLoginFormState(Settings.l_profile.field.value)

    override fun onDispose() {
        // Пользователь ушёл из настроек: открытый архив и его пароль больше не нужны.
        backup.closeArchive()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSettingsModule {
    @Binds
    @IntoMap
    @ScreenModelKey(AppSettingsSM::class)
    abstract fun bindAppSettingsSM(sm: AppSettingsSM): ScreenModel
}
