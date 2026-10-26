package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable
import com.client.xvideos.xlive.ui.following.XLiveFollowingActions

/**
 * Действия вкладки «Избранное».
 *
 * @property grid сетка любой подвкладки: открыть, убрать (в «Подписках» —
 *   отписаться), свайп вниз; подвкладку различает ScreenModel.
 * @property following вход, выход и повтор «Подписок».
 * @property records открыть и удалить запись.
 */
@Immutable
data class XLiveFavoritesActions(
    val grid: XLiveSavedActions = XLiveSavedActions(),
    val following: XLiveFollowingActions = XLiveFollowingActions(),
    val records: XLiveRecordsActions = XLiveRecordsActions(),
)
