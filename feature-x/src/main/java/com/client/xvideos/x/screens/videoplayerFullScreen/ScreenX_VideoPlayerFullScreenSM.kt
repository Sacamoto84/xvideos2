package com.client.xvideos.x.screens.videoplayerFullScreen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.normalizeXUrl
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ScreenModel полноэкранного плеера X.
 *
 * После миграции на общий [com.client.xvideos.common.videoplayer.host.MediaPlayerHost]
 * модель держит только загрузку HLS-ссылки. Управление воспроизведением, дорожками
 * и скоростью — в `MediaPlayerHost`, создаваемом в `Content()`. Стартовая позиция
 * приходит через [position] и применяется к хосту, когда медиа готово.
 */
@Deprecated("Используйте ScreenX_VideoPlayerSM с встроенным полноэкранным режимом")
@Stable
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
        loadJob?.cancel()
        isLoading = false
        Timber.d("!!! ScreenX_VideoPlayerFullScreenSM onDispose")
    }

    var passedString: String by mutableStateOf("")
        private set

    var isError: Boolean by mutableStateOf(false)
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    var playerConfig: HTML5PlayerConfig? by mutableStateOf(null)
        private set

    init {
        loadVideo()
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadVideo(forceReload: Boolean = false) {
        if (url.isBlank()) {
            isLoading = false
            isError = true
            return
        }
        loadJob?.cancel()
        isLoading = true
        isError = false
        loadJob = screenModelScope.launch {
            try {
                Timber.d("!!! ScreenX_VideoPlayerFullScreenSM loadVideo(forceReload=$forceReload)")

                if (forceReload) {
                    withContext(Dispatchers.IO) {
                        db.cacheUrlStringRam.delete(url)
                    }
                }

                // RAM-кэш (чистится при старте процесса), чтобы истекающий HLS-токен обновлялся.
                val cachedHtml = if (forceReload) null else withContext(Dispatchers.IO) {
                    db.cacheUrlStringRam.get(url)
                }
                val isFromCache = cachedHtml != null
                val htmlContent = if (cachedHtml == null) {
                    withContext(Dispatchers.IO) {
                        readHtmlFromURLDirect(url)
                    }
                } else {
                    cachedHtml.content
                }

                val (config, hls) = withContext(Dispatchers.Default) {
                    val script = parserItemVideo(htmlContent)
                    val parsedConfig = script?.let { parseHTML5Player(it) }
                    val streamUrl = parsedConfig?.videoHLS?.takeIf { it.isNotBlank() }
                        ?: parsedConfig?.videoUrlHigh?.takeIf { it.isNotBlank() }
                        ?: parsedConfig?.videoUrlLow.orEmpty()
                    val normalizedStream = if (streamUrl.isNotBlank()) normalizeXUrl(streamUrl) else ""
                    parsedConfig to normalizedStream
                }
                playerConfig = config
                passedString = hls
                if (hls.isBlank()) {
                    isError = true
                    withContext(Dispatchers.IO) {
                        db.cacheUrlStringRam.delete(url)
                    }
                } else if (!isFromCache && htmlContent.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        db.cacheUrlStringRam.put(url, htmlContent)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Страница видео не загрузилась: %s", url)
                isError = true
                withContext(Dispatchers.IO) {
                    db.cacheUrlStringRam.delete(url)
                }
            } finally {
                if (loadJob === coroutineContext[kotlinx.coroutines.Job]) {
                    isLoading = false
                }
            }
        }
    }

    fun onPlaybackError() {
        Timber.w("ScreenX_VideoPlayerFullScreenSM: ошибка воспроизведения для %s, очистка RAM-кэша", url)
        isError = true
        screenModelScope.launch(Dispatchers.IO) {
            db.cacheUrlStringRam.delete(url)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleItemFullScreen {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenX_VideoPlayerFullScreenSM.Factory::class)
    abstract fun bindHiltDetailsScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenX_VideoPlayerFullScreenSM.Factory,
    ): ScreenModelFactory
}
