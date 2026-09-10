package com.client.xvideos.common.util

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] для корутин.
 *
 * Стандартный `runCatching` ловит `Throwable`, то есть заодно проглатывает
 * `CancellationException`. В цикле загрузки это значит, что после отмены scope
 * итерация не прерывается: очередной элемент просто помечается как «не скачан»,
 * и цикл идёт дальше по уже отменённой корутине.
 *
 * Здесь отмена всегда пробрасывается наружу, остальные ошибки — как обычно,
 * в [Result.failure].
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
