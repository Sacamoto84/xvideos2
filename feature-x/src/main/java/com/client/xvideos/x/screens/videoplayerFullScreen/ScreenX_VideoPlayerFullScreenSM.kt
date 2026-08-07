package com.client.xvideos.x.screens.videoplayerFullScreen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber

/**
 * ScreenModel полноэкранного плеера X.
 *
 * После миграции на общий [com.client.xvideos.common.videoplayer.host.MediaPlayerHost]
 * модель держит только загрузку HLS-ссылки. Управление воспроизведением, дорожками
 * и скоростью — в `MediaPlayerHost`, создаваемом в `Content()`. Стартовая позиция
 * приходит через [position] и применяется к хосту, когда медиа готово.
 */
class ScreenX_VideoPlayerFullScreenSM @AssistedInject constructor(
    @Assisted val url: String,
    @Assisted val position: Long,
    val db: AppFileDatabase
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(url: String, position: Long): ScreenX_VideoPlayerFullScreenSM
    }

    override fun onDispose() {
        super.onDispose()
        Timber.e("!!! ScreenX_VideoPlayerFullScreenSM onDispose")
    }

    var passedString: String by mutableStateOf("")

    val a: MutableState<HTML5PlayerConfig?> = mutableStateOf(HTML5PlayerConfig())

    init {
        // При отказе passedString остаётся пустым — экран показывает индикатор
        // вместо того, чтобы закрыть приложение.
        screenModelScope.launchCatching(message = "Страница видео не загрузилась: $url") {
            Timber.e("!!! ScreenX_VideoPlayerFullScreenSM init()")

            // RAM-кэш (чистится при старте процесса), чтобы истекающий HLS-токен обновлялся.
            val res = db.cacheUrlStringRam.get(url)
            val s = if (res == null) {
                val content = readHtmlFromURLDirect(url)
                db.cacheUrlStringRam.put(url, content)
                content
            } else {
                res.content
            }

            val script = parserItemVideo(s)
            a.value = script?.let { parseHTML5Player(it) }
            passedString = a.value?.videoHLS.toString()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleItem {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenX_VideoPlayerFullScreenSM.Factory::class)
    abstract fun bindHiltDetailsScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenX_VideoPlayerFullScreenSM.Factory,
    ): ScreenModelFactory
}
