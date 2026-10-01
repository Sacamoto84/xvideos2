package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import kotlinx.coroutines.launch

/**
 * Нижняя панель страниц канала: выбранный номер прокручивает пейджер.
 *
 * @param pagerState Пейджер страниц видео.
 * @param maxPages Число страниц; номер вне диапазона прижимается к краю.
 */
@Composable
fun ChannelPagerBottomBar(
    pagerState: PagerState,
    maxPages: Int,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val onPageChange: (Int) -> Unit = remember(pagerState, coroutineScope, maxPages) {
        { targetPage ->
            val clamped = targetPage.coerceIn(0, (maxPages - 1).coerceAtLeast(0))
            coroutineScope.launch {
                pagerState.animateScrollToPage(clamped)
            }
        }
    }

    BottomListDashBoardNavigationButtons2(
        value = pagerState.currentPage,
        onChange = onPageChange,
        max = maxPages,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ChannelPagerBottomBarPreview() {
    ChannelPagerBottomBar(pagerState = rememberPagerState { 5 }, maxPages = 5)
}
