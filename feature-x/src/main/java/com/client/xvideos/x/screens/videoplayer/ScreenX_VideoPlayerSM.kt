package com.client.xvideos.x.screens.videoplayer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.x.extractXVideoId
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.HTML5PlayerConfig
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.model.XHistoryItem
import com.client.xvideos.x.parseDurationToMs
import com.client.xvideos.x.parcer.parseHTML5Player
import com.client.xvideos.x.parcer.parserItemVideo
import com.client.xvideos.x.parcer.parserItemVideoTags
import com.client.xvideos.x.screens.tags.ScreenTags
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.videoplayer.atom.formatTime
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ScreenModel экрана видеоплеера X.
 *
 * После миграции на общий Compose-плеер ([com.client.xvideos.common.videoplayer.host.MediaPlayerHost])
 * модель больше НЕ держит `ExoPlayer` и не управляет дорожками/скоростью напрямую —
 * этим занимается `MediaPlayerHost`, создаваемый в `Content()`. Здесь остаётся
 * только X-специфика: загрузка HTML страницы видео, извлечение HLS-ссылки и тегов,
 * навигация на теги/полный экран, а также сохранение прогресса и возобновление («Продолжить просмотр»).
 */
@Stable
class ScreenX_VideoPlayerSM @AssistedInject constructor(
    @Assisted("url") url: String,
    @Assisted("initialItem") val initialItem: ItemsX?,
    val db: AppFileDatabase,
    val saved: SavedX,
) : ScreenModel {

    constructor(url: String, db: AppFileDatabase) : this(
        url = url,
        initialItem = null,
        db = db,
        saved = SavedX(CoroutineScope(Dispatchers.Unconfined))
    )

    val url: String = normalizeXUrl(url)

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(
            @Assisted("url") url: String,
            @Assisted("initialItem") initialItem: ItemsX? = null,
        ): ScreenX_VideoPlayerSM
    }

    override fun onDispose() {
        super.onDispose()
        loadJob?.cancel()
        isLoading = false
        Timber.d("!!! ScreenVideoPlayerSM onDispose")
    }

    /** HLS-ссылка для воспроизведения (master-playlist xvideos). */
    var passedHLS: String by mutableStateOf("")
        private set

    var isError: Boolean by mutableStateOf(false)
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    /** Распарсенный конфиг html5-плеера (титул/превью/HLS и пр.). */
    var playerConfig: HTML5PlayerConfig? by mutableStateOf(null)
        private set

    /** Теги/каналы/порноактрисы для overlay поверх видео. */
    var tags by mutableStateOf(TagsModel(emptyList(), emptyList(), emptyList()))
        private set

    /** Флаг полноэкранного (ландшафтного) режима. */
    var isFullScreen: Boolean by mutableStateOf(false)
        private set

    fun toggleFullScreen() {
        isFullScreen = !isFullScreen
    }

    fun enterFullScreen() {
        isFullScreen = true
    }

    fun exitFullScreen() {
        isFullScreen = false
    }

    var currentItem: ItemsX by mutableStateOf(
        initialItem ?: ItemsX(id = extractXVideoId(url) ?: 0L, href = url)
    )
        private set

    /** Сохранённый элемент истории для данного видео (если был). */
    var historyItem: XHistoryItem? by mutableStateOf(null)
        private set

    /** Стартовая позиция в секундах, если видео подходит для возобновления. */
    var resumePositionSeconds: Float? by mutableStateOf(null)
        private set

    /** Текст уведомления о возобновлении (например, "Возобновлено с 04:12"). */
    var resumeNoticeText: String? by mutableStateOf(null)
        private set

    private fun checkAndInitResume(videoId: Long) {
        if (videoId <= 0L || resumePositionSeconds != null) return
        val item = saved.history.get(videoId) ?: return
        if (item.isEligibleForResume) {
            historyItem = item
            val sec = (item.lastPositionMs / 1000f).takeIf { it.isFinite() && it >= 0f } ?: return
            resumePositionSeconds = sec
            val totalSec = sec.toInt()
            resumeNoticeText = "Возобновлено с ${formatTime(totalSec)}"
        }
    }

    fun dismissResumeNotice() {
        resumeNoticeText = null
    }

    fun restartFromBeginning() {
        resumeNoticeText = null
        resumePositionSeconds = 0f
    }

    fun saveProgress(positionSeconds: Float, durationSeconds: Int) {
        val playerDurationMs = durationSeconds.coerceAtLeast(0) * 1000L
        val parsedDurationMs = parseDurationToMs(currentItem.duration)
        val durationMs = if (playerDurationMs > 0L) playerDurationMs else parsedDurationMs
        val safeSeconds = positionSeconds.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val positionMs = (safeSeconds * 1000f).toLong().coerceIn(0L, maxPos)
        val videoId = currentItem.id.takeIf { it > 0L } ?: (extractXVideoId(url) ?: 0L)
        if (videoId > 0L) {
            val itemToSave = if (currentItem.id > 0L) currentItem else currentItem.copy(id = videoId)
            saved.history.updateProgress(itemToSave, positionMs, durationMs)
        }
    }

    init {
        val initialId = currentItem.id.takeIf { it > 0L } ?: (extractXVideoId(url) ?: 0L)
        if (initialId > 0L) {
            checkAndInitResume(initialId)
        }
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
                Timber.d("!!! ScreenVideoPlayerSM loadVideo(forceReload=$forceReload)")

                if (forceReload) {
                    withContext(Dispatchers.IO) {
                        db.cacheUrlStringRam.delete(url)
                    }
                }

                // RAM-кэш чистится при старте процесса (clearVolatileCachesOnProcessStart),
                // поэтому HLS-ссылки с истекающим токеном обновятся после перезапуска.
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

                val parsedData = withContext(Dispatchers.Default) {
                    parseVideoPageData(htmlContent)
                }

                playerConfig = parsedData.config
                tags = parsedData.tags
                passedHLS = parsedData.streamCandidate

                val parsedConfig = parsedData.config
                val resolvedId = currentItem.id.takeIf { it > 0L }
                    ?: parsedData.pageVideoId
                    ?: (extractXVideoId(url) ?: 0L)
                if (parsedConfig != null || resolvedId > 0L) {
                    currentItem = currentItem.copy(
                        id = resolvedId,
                        title = currentItem.title.ifBlank { parsedConfig?.videoTitle.orEmpty() },
                        duration = currentItem.duration.ifBlank { parsedData.pageDuration },
                        previewImage = currentItem.previewImage.ifBlank {
                            parsedConfig?.thumbUrl169?.ifBlank { parsedConfig.thumbUrl }.orEmpty()
                        },
                        href = url
                    )
                    if (resolvedId > 0L) {
                        checkAndInitResume(resolvedId)
                    }
                }
                if (parsedData.streamCandidate.isBlank()) {
                    isError = true
                    isFullScreen = false
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
                isFullScreen = false
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
        Timber.w("ScreenX_VideoPlayerSM: ошибка воспроизведения для %s, очистка RAM-кэша", url)
        isError = true
        isFullScreen = false
        screenModelScope.launch(Dispatchers.IO) {
            db.cacheUrlStringRam.delete(url)
        }
    }

    /**
     * ## Открыть экран с нужным тегом
     */
    fun openTag(tag: String, navigator: Navigator) {
        if (tag.isNotBlank()) {
            navigator.push(ScreenTags(tag.trim()))
        }
    }

    /**
     * ## Открыть плеер в полном окне
     * @deprecated Используйте [toggleFullScreen] или [enterFullScreen]: плеер переключается в ландшафт на месте.
     */
    @Suppress("UnusedParameter")
    @Deprecated("Используйте toggleFullScreen() или enterFullScreen()")
    fun openFullScreen(navigator: Navigator? = null, positionMs: Long = -1L) {
        enterFullScreen()
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

private data class ParsedVideoData(
    val config: HTML5PlayerConfig?,
    val tags: TagsModel,
    val streamCandidate: String,
    val pageVideoId: Long?,
    val pageDuration: String,
)

private fun parseVideoPageData(htmlContent: String): ParsedVideoData {
    val document = org.jsoup.Jsoup.parse(htmlContent)
    val script = parserItemVideo(document)
    val config = script?.let { parseHTML5Player(it) }
    val parsedTags = parserItemVideoTags(document)
    val hls = config?.videoHLS?.takeIf { it.isNotBlank() }
        ?: config?.videoUrlHigh?.takeIf { it.isNotBlank() }
        ?: config?.videoUrlLow.orEmpty()
    val streamCandidate = if (hls.isNotBlank()) normalizeXUrl(hls) else ""
    val pageId = document.selectFirst("#video-player-bg")?.attr("data-id")?.toLongOrNull()
        ?: document.selectFirst("[data-id]")?.attr("data-id")?.toLongOrNull()
    val pageDuration = document.selectFirst("span.duration")?.text().orEmpty()
    return ParsedVideoData(config, parsedTags, streamCandidate, pageId, pageDuration)
}

