package com.client.xvideos.x.screens.videoplayer

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.x.feature.saved.SavedX
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * ScreenModel экрана локального воспроизведения скачанных роликов X.
 *
 * Предоставляет доступ к локальным сохранениям, истории и загрузкам ([SavedX]).
 *
 * @property saved Фасад локальных данных раздела X.
 */
@Stable
class ScreenX_LocalVideoPlayerSM @Inject constructor(
    val saved: SavedX
) : ScreenModel

/**
 * Hilt-модуль привязки [ScreenX_LocalVideoPlayerSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLocalVideoPlayer {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenX_LocalVideoPlayerSM::class)
    abstract fun bindHiltLocalVideoPlayerScreenModel(
        hiltLocalVideoPlayerScreenModel: ScreenX_LocalVideoPlayerSM
    ): ScreenModel
}
