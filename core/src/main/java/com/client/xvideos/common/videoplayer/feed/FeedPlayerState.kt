package com.client.xvideos.common.videoplayer.feed

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerPool
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import com.client.xvideos.common.videoplayer.net.VideoHttpDataSource
import timber.log.Timber

/**
 * Пул плееров + менеджер предзагрузки для вертикальной ленты.
 *
 * Раньше каждая страница пейджера создавала собственный `ExoPlayer`
 * (`rememberExoPlayerWithLifecycle`), и при `beyondViewportPageCount = 2` в памяти
 * жило до пяти плееров с декодерами. Здесь плееров ровно [poolCapacity], страницы
 * берут их из [playerPool] и возвращают при уходе из композиции, а соседние
 * элементы греет [preloadManager] без своих плееров.
 *
 * Все методы — только с главного потока (требование `PlayerPool`).
 */
@MainThread
@OptIn(UnstableApi::class)
class FeedPlayerState(
    context: Context,
    private val poolCapacity: Int = DEFAULT_POOL_CAPACITY,
) {
    private val appContext = context.applicationContext

    private val statusControl =
        object : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
            var currentPlayingIndex: Int = C.INDEX_UNSET

            override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
                val tier = FeedPreloadPolicy.tierFor(rankingData, currentPlayingIndex, poolCapacity)
                // Длительность берём у самого уровня — так её нельзя перепутать
                // с чужой. Отличается только назначение отрезка: ближние уровни
                // держим в памяти, дальний — на диске.
                return when (tier) {
                    FeedPreloadTier.NEAR_LOADED, FeedPreloadTier.FAR_LOADED ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(tier.durationMs)

                    FeedPreloadTier.CACHED_ONLY ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeCached(tier.durationMs)
                }
            }
        }

    // Не голая http-фабрика: `DefaultPreloadManager` подставляет её как есть,
    // вместо своего `DefaultDataSource.Factory`, и локальные файлы (скачанные
    // ролики отдаются голым путём, без схемы) ушли бы в Ktor как HTTP-запрос.
    // `DefaultDataSource` разбирает схему и уводит локальные файлы в
    // `FileDataSource`, а сеть — в Ktor.
    private val dataSourceFactory = DefaultDataSource.Factory(appContext, VideoHttpDataSource.factory())

    private val builder: DefaultPreloadManager.Builder =
        DefaultPreloadManager.Builder(appContext, statusControl)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 5_000,
                        /* maxBufferMs = */ 20_000,
                        /* bufferForPlaybackMs = */ 500,
                        /* bufferForPlaybackAfterRebufferMs = */
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setCache(FeedVideoCache.get(appContext))
            .setDataSourceFactory(dataSourceFactory)

    /**
     * Пул плееров ленты.
     *
     * Внимание: `PlayerPool.yield()` при возврате плеера сбрасывает только
     * `playWhenReady`, `stop()` и `clearMediaItems()`. Любое другое свойство,
     * выставленное на плеере страницей (громкость, видеоэффекты, скорость),
     * страница обязана снять сама через `playerTeardown` у `rememberPooledPlayer` —
     * пул за этим не следит.
     */
    val playerPool: PlayerPool<ExoPlayer> = PlayerPool(poolCapacity) {
        builder.buildExoPlayer().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            setHandleAudioBecomingNoisy(true)
            // Держим кодеки прогретыми между страницами — иначе выигрыш от пула
            // съедается пересозданием декодера на каждом свайпе.
            setForegroundMode(true)
        }
    }

    val preloadManager: DefaultPreloadManager = builder.build()

    /** Учёт отданного в прогрев. Вся арифметика — в [FeedPreloadRegistry]. */
    private val registry = FeedPreloadRegistry<MediaItem>()

    /**
     * Применить решение реестра к менеджеру прогрева.
     *
     * `Replace` снимает старый элемент перед добавлением нового: без этого
     * `BasePreloadManager` положил бы новый holder поверх старого через `put()`,
     * не освободив предыдущий.
     *
     * Имя не `apply`: так называется scope-функция стандартной библиотеки, и
     * затенять её членом класса — верный способ получить неочевидный вызов.
     */
    private fun applyAction(index: Int, action: PreloadAction<MediaItem>) {
        when (action) {
            is PreloadAction.None -> Unit
            is PreloadAction.Add -> preloadManager.add(action.item, index)
            is PreloadAction.Replace -> {
                preloadManager.remove(action.old)
                preloadManager.add(action.new, index)
            }
        }
    }

    /**
     * Ключ элемента для preload-менеджера. `mediaId` — позиция в ленте (по нему
     * менеджер удаляет элементы), `customCacheKey` — сам url, чтобы дисковый кеш
     * переживал перетасовку списка.
     *
     * `customCacheKey` ставим только прогрессивным потокам. Адаптивным его
     * запрещает `DownloadRequest` (`customCacheKey must be null for type: 2`), а
     * до него доходит `PreCacheHelper` на уровне прогрева `specifiedRangeCached` —
     * с ключом приложение падало на первом же дальнем элементе ленты. Плейлист
     * и сегменты HLS кешируются по своим адресам, отдельный ключ им не нужен.
     *
     * Тип определяет `Util.inferContentType` — то же, чем пользуется сам media3.
     * Проверка `endsWith(".m3u8")` ломалась бы на подписанном url с `?token=…`
     * и на редиректе, то есть ровно там, где краш и вернулся бы.
     */
    fun mediaItemFor(index: Int, url: String): MediaItem {
        val uri = Uri.parse(url)
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(index.toString())
            .apply {
                if (Util.inferContentType(uri) == C.CONTENT_TYPE_OTHER) setCustomCacheKey(url)
            }
            .build()
    }

    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        val action = registry.track(index, mediaItem)
        applyAction(index, action)
        // add() сам по себе только кладёт holder в карту. Без invalidate()
        // менеджер не пересобирает очередь приоритетов, и добавленный страницей
        // элемент не греется до следующего внешнего invalidate — а на одиночном
        // экране такого вызова нет вовсе.
        if (action !is PreloadAction.None) preloadManager.invalidate()
        val preloaded = preloadManager.getMediaSource(mediaItem)
        if (preloaded != null) return preloaded
        // Метод зовётся из playerSetup прямо во время свайпа. Уронить приложение
        // здесь нельзя: играем мимо прогрева тем же стеком датасорсов.
        Timber.w("preloadManager не отдал источник для ${mediaItem.mediaId} — играем мимо прогрева")
        return fallbackSourceFactory.createMediaSource(mediaItem)
    }

    /**
     * Запасная фабрика на случай, когда preload-менеджер не отдал источник.
     * Тот же [dataSourceFactory], что и у прогрева, — иначе локальные файлы
     * снова ушли бы в Ktor как HTTP-запрос.
     */
    private val fallbackSourceFactory: MediaSource.Factory =
        DefaultMediaSourceFactory(appContext).setDataSourceFactory(dataSourceFactory)

    fun updateCurrentPage(index: Int) {
        statusControl.currentPlayingIndex = index
        // Отдельный invalidate() не нужен: setCurrentPlayingIndex доходит до
        // SimpleRankingDataComparator, а тот при смене индекса синхронно дёргает
        // InvalidationListener, который BasePreloadManager в конструкторе повесил
        // на собственный invalidate().
        preloadManager.setCurrentPlayingIndex(index)
    }

    /**
     * Держать ли кодеки прогретыми.
     *
     * `setForegroundMode(true)` удерживает декодеры даже на снятом `playWhenReady`,
     * поэтому на уходе приложения в фон режим нужно снимать явно — иначе плееры
     * пула держат аппаратные декодеры всё время, пока приложение свёрнуто.
     */
    fun setForegroundMode(foreground: Boolean) {
        playerPool.executeForAll { setForegroundMode(foreground) }
    }

    /** Элементы вошли в окно вокруг текущей страницы. `urlAt` возвращает null, если элемент ещё не подгружен пейджингом. */
    fun addRange(indices: IntRange, urlAt: (Int) -> String?) {
        indices.forEach { index ->
            val url = urlAt(index)
            if (url == null) {
                registry.markPending(index)
                return@forEach
            }
            applyAction(index, registry.track(index, mediaItemFor(index, url)))
        }
        preloadManager.invalidate()
    }

    /** Догреть индексы, у которых url не было в момент входа в окно. */
    fun retryPending(urlAt: (Int) -> String?) {
        val waiting = registry.pendingIndices()
        if (waiting.isEmpty()) return
        var resolvedAny = false
        waiting.forEach { index ->
            val url = urlAt(index) ?: return@forEach
            applyAction(index, registry.track(index, mediaItemFor(index, url)))
            resolvedAny = true
        }
        if (resolvedAny) preloadManager.invalidate()
    }

    /** Элементы вышли из окна — снимаем с прогрева по собственному учёту. */
    fun removeRange(indices: IntRange) {
        indices.forEach { index ->
            val mediaItem = registry.forget(index) ?: return@forEach
            preloadManager.remove(mediaItem)
        }
    }

    /** Кеш ([FeedVideoCache]) намеренно не трогаем: он процессный. */
    fun release() {
        registry.clear()
        playerPool.release()
        preloadManager.release()
    }

    companion object {
        const val DEFAULT_POOL_CAPACITY = 3
    }
}

@Composable
fun rememberFeedPlayerState(
    poolCapacity: Int = FeedPlayerState.DEFAULT_POOL_CAPACITY,
): FeedPlayerState {
    val context = LocalContext.current
    val state = remember(context, poolCapacity) { FeedPlayerState(context, poolCapacity) }
    LifecycleStartEffect(state) {
        state.setForegroundMode(true)
        onStopOrDispose { state.setForegroundMode(false) }
    }
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}
