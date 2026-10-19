package com.client.xvideos.x.screens.dashboards.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable

/**
 * Прокрутка страниц «Сохранённого», кроме «Избранного» (его сетка — в
 * `ScreenFavoritesSM`). Держит её ScreenModel дашбордов, а не композиция страниц:
 * ролик открывается в корневом навигаторе, дашборды уходят из композиции, и после
 * «Назад» список вставал в начало.
 */
@Stable
class SavedTabListStates {
    val downloads = LazyListState()
    val history = LazyGridState()
    val channels = LazyListState()
    val models = LazyListState()
}
