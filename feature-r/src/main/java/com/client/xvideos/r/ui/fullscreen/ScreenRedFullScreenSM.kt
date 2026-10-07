package com.client.xvideos.r.ui.fullscreen

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.fullscreen.model.RedFullScreenPlayerState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * [ScreenModel] для экрана полноэкранного воспроизведения R (TikTok-подобный вертикальный пейджер).
 *
 * Держит зависимости экрана и состояние плеера [player]. Экран раздаёт их молекулам
 * по отдельности — сам ScreenModel в молекулы не передаётся.
 */
@Stable
class ScreenRedFullScreenSM @Inject constructor(
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val search: R_SearchExplorer,
) : ScreenModel {

    /** Состояние плеера: воспроизведение, звук, скорость, петля A-B, активные контролы. */
    val player = RedFullScreenPlayerState()
}

/** Hilt-модуль привязки [ScreenRedFullScreenSM] в карту ScreenModel Voyager. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedFullScreen {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedFullScreenSM::class)
    abstract fun bindScreenRedFullScreenModel(screenModel: ScreenRedFullScreenSM): ScreenModel
}

/**
 * Валидирует и ограничивает временную метку точки A/B:
 * гарантирует неотрицательность, конечность и непревышение длительности видео.
 */
internal fun sanitizePointTime(time: Float, durationSec: Int): Float {
    if (!time.isFinite() || time < 0f) return 0f
    return if (durationSec > 0) time.coerceAtMost(durationSec.toFloat()) else time
}
