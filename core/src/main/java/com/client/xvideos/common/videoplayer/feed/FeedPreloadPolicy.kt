package com.client.xvideos.common.videoplayer.feed

import kotlin.math.abs

/** Насколько глубоко готовим элемент ленты к воспроизведению. */
enum class FeedPreloadTier {
    /** Текущая страница и прямые соседи: держим готовый к старту отрезок в памяти. */
    NEAR_LOADED,

    /** Ещё в пределах ёмкости пула: короткий отрезок, чтобы свайп не упирался в сеть. */
    FAR_LOADED,

    /** Далеко: только тянем начало файла в дисковый кеш, память не занимаем. */
    CACHED_ONLY,
}

/**
 * Чистая логика приоритетов предзагрузки ленты. Media3-типов тут нет намеренно:
 * так правило проверяется обычным JVM-тестом, а маппинг в
 * `DefaultPreloadManager.PreloadStatus` живёт в [FeedPlayerState].
 */
object FeedPreloadPolicy {

    const val NEAR_LOADED_MS = 3_000L
    const val FAR_LOADED_MS = 1_000L
    const val CACHED_ONLY_MS = 5_000L

    /**
     * @param itemIndex индекс элемента ленты, для которого считаем приоритет.
     * @param currentIndex индекс текущей страницы пейджера; отрицательное значение
     *        (`C.INDEX_UNSET`) означает «страница ещё не определилась».
     * @param poolCapacity размер пула плееров.
     */
    fun tierFor(itemIndex: Int, currentIndex: Int, poolCapacity: Int): FeedPreloadTier {
        if (currentIndex < 0) return FeedPreloadTier.CACHED_ONLY
        return when (abs(itemIndex - currentIndex)) {
            0, 1 -> FeedPreloadTier.NEAR_LOADED
            in 2..poolCapacity -> FeedPreloadTier.FAR_LOADED
            else -> FeedPreloadTier.CACHED_ONLY
        }
    }
}
