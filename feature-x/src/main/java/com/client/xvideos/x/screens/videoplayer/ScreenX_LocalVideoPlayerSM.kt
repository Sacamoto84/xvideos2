package com.client.xvideos.x.screens.videoplayer

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.parseDurationToMs
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

    fun saveProgress(
        item: ItemsX,
        currentTimeSeconds: Float,
        totalTimeSeconds: Int,
    ) {
        val playerDurationMs = totalTimeSeconds.coerceAtLeast(0) * 1000L
        val parsedDurationMs = parseDurationToMs(item.duration)
        val durationMs = if (playerDurationMs > 0L) playerDurationMs else parsedDurationMs
        val safeSeconds = currentTimeSeconds.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val positionMs = (safeSeconds * 1000f).toLong().coerceIn(0L, maxPos)
        if (item.id > 0L) {
            saved.history.updateProgress(item, positionMs, durationMs)
        }
    }
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
