package com.client.xvideos.common.videoplayer.feed

import kotlin.math.abs

/**
 * Насколько глубоко готовим элемент ленты к воспроизведению.
 *
 * @property durationMs длина отрезка от начала ролика, который готовим на этом
 *   уровне. Значения разных уровней сравнимы только внутри своей шкалы: у
 *   [NEAR_LOADED] и [FAR_LOADED] отрезок держится в памяти
 *   (`specifiedRangeLoaded`), у [CACHED_ONLY] — на диске (`specifiedRangeCached`).
 */
enum class FeedPreloadTier(val durationMs: Long) {
    /** Текущая страница и прямые соседи: держим готовый к старту отрезок в памяти. */
    NEAR_LOADED(3_000L),

    /** Дистанция не больше ёмкости пула: короткий отрезок, чтобы свайп не упирался в сеть. */
    FAR_LOADED(1_000L),

    /**
     * Далеко: только тянем начало файла в дисковый кеш, память не занимаем.
     * Отрезок здесь длиннее, чем у ближних уровней, и это не опечатка: дисковый
     * кеш не отъедает оперативную память, поэтому запас дешёвый — при быстрой
     * прокрутке ролик стартует уже с диска, а не из сети.
     */
    CACHED_ONLY(5_000L),
}

/**
 * Чистая логика приоритетов предзагрузки ленты. Media3-типов тут нет намеренно:
 * так правило проверяется обычным JVM-тестом, а маппинг в
 * `DefaultPreloadManager.PreloadStatus` живёт в [FeedPlayerState].
 */
object FeedPreloadPolicy {

    /**
     * @param itemIndex индекс элемента ленты, для которого считаем приоритет.
     * @param currentIndex индекс текущей страницы пейджера; отрицательное значение
     *        (`C.INDEX_UNSET`) означает «страница ещё не определилась».
     * @param poolCapacity размер пула плееров. При ёмкости 1 и меньше диапазон
     *        `2..poolCapacity` пуст, и уровень [FeedPreloadTier.FAR_LOADED] не
     *        выдаётся вовсе: греть в память нечем.
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
