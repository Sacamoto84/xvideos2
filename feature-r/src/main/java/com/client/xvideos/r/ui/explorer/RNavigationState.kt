package com.client.xvideos.r.ui.explorer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.r.model.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Глобальное состояние навигации и скролла для разделов RedGifs.
 *
 * Сохраняет активные вкладки, сортировку и позицию скролла каталога ниш
 * между переходами по экранам приложения.
 */
@Stable
@Singleton
class RNavigationState @Inject constructor() {
    /** Индекс текущей активной корневой вкладки (Explorer, Likes, Subscriptions и т.д.). */
    var rootTab by mutableIntStateOf(0)
    /** Индекс текущей активной сохраненной вкладки. */
    var savedTab by mutableIntStateOf(0)

    /** Текущая выбранная сортировка в каталоге ниш. */
    var nichesSort by mutableStateOf(Order.NICHES_SUBSCRIBERS_D)
        private set

    /** Индекс первого видимого элемента списка ниш для восстановления скролла. */
    var nichesFirstVisibleItemIndex by mutableIntStateOf(0)
        private set

    /** Смещение первого видимого элемента списка ниш в пикселях. */
    var nichesFirstVisibleItemScrollOffset by mutableIntStateOf(0)
        private set

    /** Сохраняет текущую позицию скролла в каталоге ниш. */
    fun updateNichesScrollPosition(index: Int, offset: Int) {
        nichesFirstVisibleItemIndex = index.coerceAtLeast(0)
        nichesFirstVisibleItemScrollOffset = offset.coerceAtLeast(0)
    }

    /** Обновляет порядок сортировки ниш. */
    fun updateNichesSort(order: Order) {
        nichesSort = order
    }

    /** Сбрасывает сохраненную позицию скролла ниш в начало. */
    fun resetNichesScrollPosition() {
        nichesFirstVisibleItemIndex = 0
        nichesFirstVisibleItemScrollOffset = 0
    }
}

/** CompositionLocal для предоставления [RNavigationState] в иерархию Compose. */
val LocalRNavigationState = compositionLocalOf { RNavigationState() }
