package com.client.xvideos.l.ui.screens.screenAlbumList

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateOf

/**
 * Пейджер, у которого число страниц приходит извне и меняется по ходу загрузки.
 */
class DefaultPagerState1(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
) : PagerState(currentPage, currentPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()
}
