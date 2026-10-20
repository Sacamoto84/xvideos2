package com.client.xvideos.x.screens.dashboards.model

import androidx.compose.runtime.Stable
import com.client.xvideos.common.ui.scroll.LazyGridPosition
import com.client.xvideos.common.ui.scroll.LazyListPosition

/**
 * Позиции списков страниц «Сохранённого», кроме «Избранного» (его сетка — в
 * `ScreenFavoritesSM`). Держит их ScreenModel дашбордов, а не композиция страниц:
 * ролик открывается в корневом навигаторе, дашборды уходят из композиции, и после
 * «Назад» список вставал в начало. Только позиции — см. [LazyListPosition].
 */
@Stable
class SavedTabListPositions {
    val downloads = LazyListPosition()
    val history = LazyGridPosition()
    val channels = LazyListPosition()
    val models = LazyListPosition()
}
