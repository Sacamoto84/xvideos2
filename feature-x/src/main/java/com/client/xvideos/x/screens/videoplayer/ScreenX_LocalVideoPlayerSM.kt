package com.client.xvideos.x.screens.videoplayer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
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
) : ScreenModel {

    /**
     * Полноэкранный (альбомный) режим — как у плеера ролика из сети. Здесь, а
     * не в `remember`: корневой навигатор не хранит состояние экранов.
     */
    var isFullScreen: Boolean by mutableStateOf(false)
        private set

    /** Переключает полноэкранный режим. */
    fun toggleFullScreen() {
        isFullScreen = !isFullScreen
    }

    /** Выходит из полноэкранного режима. */
    fun exitFullScreen() {
        isFullScreen = false
    }

    fun saveProgress(
        item: ItemsX,
        currentTimeSeconds: Float,
        totalTimeSeconds: Int,
    ) {
        saved.history.savePlayerProgress(item, currentTimeSeconds, totalTimeSeconds)
    }

    /**
     * Позиция старта в секундах: сохранённая позиция возобновления либо 0 — с начала.
     * История может читаться с диска, поэтому функция приостанавливающая.
     */
    suspend fun startPositionSeconds(videoId: Long): Float =
        saved.history.resumePositionSeconds(videoId) ?: 0f
}

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
