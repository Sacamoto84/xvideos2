package com.client.xvideos.r.ui.explorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.r.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RNavigationState @Inject constructor() {
    var nichesSort by mutableStateOf(Order.NICHES_SUBSCRIBERS_D)
        private set

    var nichesFirstVisibleItemIndex by mutableIntStateOf(0)
        private set

    var nichesFirstVisibleItemScrollOffset by mutableIntStateOf(0)
        private set

    fun updateNichesScrollPosition(index: Int, offset: Int) {
        nichesFirstVisibleItemIndex = index.coerceAtLeast(0)
        nichesFirstVisibleItemScrollOffset = offset.coerceAtLeast(0)
    }

    fun updateNichesSort(order: Order) {
        nichesSort = order
    }

    fun resetNichesScrollPosition() {
        nichesFirstVisibleItemIndex = 0
        nichesFirstVisibleItemScrollOffset = 0
    }
}
