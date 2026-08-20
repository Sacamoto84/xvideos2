package com.client.xvideos.x.screens.videoplayer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.util.launchCatching
import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import com.client.xvideos.x.parcer.parserItemVideoTags
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.screens.tags.ScreenTags
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.screens.videoplayerFullScreen.ScreenX_VideoPlayerFullScreen
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ScreenModel экрана видеоплеера X.
 *
 * После миграции на общий Compose-плеер ([com.client.xvideos.common.videoplayer.host.MediaPlayerHost])
 * модель больше НЕ держит `ExoPlayer` и не управляет дорожками/скоростью напрямую —
 * этим занимается `MediaPlayerHost`, создаваемый в `Content()`. Здесь остаётся
 * только X-специфика: загрузка HTML страницы видео, извлечение HLS-ссылки и тегов,
 * навигация на теги/полный экран и приём позиции, возвращаемой из fullscreen.
 */
class ScreenX_VideoPlayerSM @AssistedInject constructor(
    @Assisted val url: String,
    val db: AppFileDatabase
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(url: String): ScreenX_VideoPlayerSM
    }

    override fun onDispose() {
        super.onDispose()
        Timber.e("!!! ScreenVideoPlayerSM onDispose")
    }

    /** HLS-ссылка для воспроизведения (master-playlist xvideos). */
    var passedHLS: String by mutableStateOf("")

    /** Распарсенный конфиг html5-плеера (титул/превью/HLS и пр.). */
    val a: MutableState<HTML5PlayerConfig?> = mutableStateOf(HTML5PlayerConfig())

    /** Теги/каналы/порноактрисы для overlay поверх видео. */
    var tags by mutableStateOf(TagsModel(emptyList(), emptyList(), emptyList()))

    /** Позиция (мс), возвращённая из полноэкранного режима; -1 — нет. */
    var positionFromFullscreen by mutableLongStateOf(-1L)

    init {
        // Возврат позиции из полноэкранного экрана. Подписка стояла внутри
        // блока загрузки, ниже по коду: при отказе сети до неё не доходило.
        // От сети она не зависит — держим отдельно.
        screenModelScope.launch {
            EventBus.events
                .filterIsInstance<Event.X_FullScreenExitPosition>()
                .collect { event ->
                    Timber.i("!!! ~~~ collect Event.X_FullScreenExitPosition ${event.position}")
                    positionFromFullscreen = event.position
                }
        }

        screenModelScope.launchCatching(message = "Страница видео не загрузилась: $url") {

            Timber.e("!!! ScreenVideoPlayerSM init()")

            // RAM-кэш чистится при старте процесса (clearVolatileCachesOnProcessStart),
            // поэтому HLS-ссылки с истекающим токеном обновятся после перезапуска.
            // ROM-кэш хранил страницу вечно → протухший токен ломал воспроизведение.
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

            // Список тегов
            tags = parserItemVideoTags(s)

            passedHLS = a.value?.videoHLS.toString()
        }
    }

    /**
     * ## Открыть экран с нужным тегом
     */
    fun openTag(tag: String, navigator: Navigator) {
        navigator.push(ScreenTags(tag))
    }

    /**
     * ## Открыть плеер в полном окне
     * @param positionMs текущая позиция воспроизведения (мс), берётся из MediaPlayerHost.
     */
    fun openFullScreen(navigator: Navigator, positionMs: Long) {
        navigator.push(ScreenX_VideoPlayerFullScreen(url, positionMs))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleItem {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenX_VideoPlayerSM.Factory::class)
    abstract fun bindHiltDetailsScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenX_VideoPlayerSM.Factory,
    ): ScreenModelFactory
}
