package com.client.xvideos.r.ui.ui.lazyrow123

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

/**
 * Сбой загрузки, который список показывает строкой с «Повторить» в своём конце.
 *
 * Сбой обновления попадает сюда и у непустого списка: пока обновление не
 * повторят, Paging не запрашивает следующие страницы, и без строки лента молча
 * обрывается на уже загруженном.
 */
internal fun listLoadError(loadState: CombinedLoadStates): Throwable? =
    (loadState.append as? LoadState.Error)?.error ?: (loadState.refresh as? LoadState.Error)?.error

/**
 * Сбой обновления, о котором пользователю сообщают сразу, не дожидаясь конца списка.
 *
 * У пустого списка строка ошибки — единственное содержимое экрана, второе
 * сообщение там лишнее.
 *
 * @param announced Сбой, о котором уже сообщили: состояние загрузки переживает
 * экран, и тот же сбой приходит снова при возврате на него.
 */
internal fun refreshFailureToAnnounce(refresh: LoadState, itemCount: Int, announced: Throwable?): Throwable? =
    (refresh as? LoadState.Error)?.error?.takeIf { itemCount > 0 && it !== announced }
