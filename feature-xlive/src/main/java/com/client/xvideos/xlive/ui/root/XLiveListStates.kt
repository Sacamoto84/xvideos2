package com.client.xvideos.xlive.ui.root

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable

/**
 * Состояния прокрутки вкладок одним значением: по отдельности они раздували
 * сигнатуру экрана сверх порога detekt. Сами позиции живут в ScreenModel
 * (`LazyGridPosition`), сюда попадают состояния на время композиции.
 *
 * @property rooms сетка «Эфиров».
 * @property tags таблица «Тегов».
 * @property saved сетка показанной подвкладки «Избранного».
 * @property search сетка результата «Поиска».
 */
@Stable
data class XLiveListStates(
    val rooms: LazyGridState,
    val tags: LazyListState,
    val saved: LazyGridState,
    val search: LazyGridState,
)
