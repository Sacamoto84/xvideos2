package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.ui.following.XLiveFollowingState

/**
 * Вкладка «Избранное» одним значением: по отдельности подвкладки раздували
 * сигнатуру экрана сверх порога detekt.
 *
 * @property saved показанная подвкладка из «Моих» и «Истории».
 * @property following «Подписки».
 * @property records «Записи».
 */
@Immutable
data class XLiveFavoritesState(
    val saved: XLiveSavedState = XLiveSavedState(),
    val following: XLiveFollowingState = XLiveFollowingState(),
    val records: XLiveRecordsState = XLiveRecordsState(),
)
