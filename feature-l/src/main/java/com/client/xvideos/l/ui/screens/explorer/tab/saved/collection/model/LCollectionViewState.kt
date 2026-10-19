package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionItemsOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter

/**
 * Открытая коллекция: сетка, отбор и порядок. Держит её ScreenModel, а не композиция:
 * полноэкранный просмотр открывается в корневом навигаторе, экран уходит из
 * композиции, и после «Назад» отбор сбрасывался на «Все», а сетка вставала в начало.
 */
@Stable
class LCollectionViewState(val collectionName: String) {
    val host = LazyRowPictureDetailsHost(collectionName)
    var typeFilter by mutableStateOf(LCollectionTypeFilter.ALL)
    var order by mutableStateOf(LCollectionItemsOrder.NEWEST)
}
