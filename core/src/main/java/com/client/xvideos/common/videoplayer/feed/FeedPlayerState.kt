package com.client.xvideos.common.videoplayer.feed

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerPool
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl

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

    /**
     * Что именно мы отдали в прогрев: индекс в ленте → добавленный `MediaItem`.
     *
     * Собрать `MediaItem` заново по данным пейджинга в момент удаления нельзя:
     * к тому времени, когда индекс покидает окно, пейджинг уже мог выбросить
     * этот элемент из списка — url не найдётся, `remove` не вызовется, и
     * источник останется в preload-менеджере навсегда. Поэтому помним сами.
     */
    private val preloadedItems = mutableMapOf<Int, MediaItem>()

    /**
     * Ключ элемента для preload-менеджера. `mediaId` — позиция в ленте (по нему
     * менеджер удаляет элементы), `customCacheKey` — сам url, чтобы дисковый кеш
     * переживал перетасовку списка.
     */
    fun mediaItemFor(index: Int, url: String): MediaItem =
        MediaItem.Builder()
            .setUri(url)
            .setMediaId(index.toString())
            .setCustomCacheKey(url)
            .build()

    /** Источник для страницы: уже прогретый, либо добавленный сейчас. */
    fun mediaSourceFor(mediaItem: MediaItem, index: Int): MediaSource {
        preloadManager.getMediaSource(mediaItem)?.let { return it }
        preloadManager.add(mediaItem, index)
        preloadedItems[index] = mediaItem
        return checkNotNull(preloadManager.getMediaSource(mediaItem)) {
            "preloadManager не отдал источник для ${mediaItem.mediaId}"
        }
    }

    fun updateCurrentPage(index: Int) {
        statusControl.currentPlayingIndex = index
        preloadManager.setCurrentPlayingIndex(index)
        // setCurrentPlayingIndex только обновляет компаратор ранжирования, пересчёт
        // целей он не запускает. Без invalidate свайп внутри уже добавленного окна
        // оставил бы приоритеты протухшими: новый текущий ролик не поднялся бы в
        // NEAR_LOADED.
        preloadManager.invalidate()
    }

    /** Элементы вошли в окно вокруг текущей страницы. `urlAt` возвращает null, если элемент ещё не подгружен пейджингом. */
    fun addRange(indices: IntRange, urlAt: (Int) -> String?) {
        indices.forEach { index ->
            val url = urlAt(index) ?: return@forEach
            val mediaItem = mediaItemFor(index, url)
            preloadManager.add(mediaItem, index)
            preloadedItems[index] = mediaItem
        }
        preloadManager.invalidate()
    }

    /** Элементы вышли из окна — снимаем с прогрева по собственному учёту. */
    fun removeRange(indices: IntRange) {
        indices.forEach { index ->
            val mediaItem = preloadedItems.remove(index) ?: return@forEach
            preloadManager.remove(mediaItem)
        }
    }

    /** Кеш ([FeedVideoCache]) намеренно не трогаем: он процессный. */
    fun release() {
        preloadedItems.clear()
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
    DisposableEffect(state) { onDispose { state.release() } }
    return state
}
