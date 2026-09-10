package com.client.xvideos.x.screens.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.urlStart
import kotlinx.coroutines.launch

class ScreenTags(private val tag: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenTagsViewModel, ScreenTagsViewModel.Factory> { factory -> factory.create(tag) }
        val navigator = LocalNavigator.currentOrThrow
        val job = rememberCoroutineScope()

        // Число страниц приходит с нулевой страницей; до её разбора пейджер
        // держит одну. pageCount читается лениво, поэтому рост с 1 до 149
        // пейджер подхватывает без пересоздания состояния.
        val pagerState = rememberPagerState(initialPage = 0) { vm.screen.lastPage }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    Text(tag)
                    Row {
                        Text(vm.screen.title0 + " ")
                        Text(vm.screen.title1, color = Color(0xFF787878))
                    }
                }
            },
            bottomBar = {
                // Без кнопки страны, в отличие от ленты раздела: адрес
                // /tags/<тег>/N от страны не зависит.
                BottomListDashBoardNavigationButtons2(
                    value = pagerState.currentPage,
                    onChange = { job.launch { pagerState.animateScrollToPage(it) } },
                    max = vm.screen.lastPage,
                )
            },
        ) { padding ->
            // Раньше padding игнорировался (`{ _ -> }`) — список рисовался под
            // topBar'ом, и его первые строки оказывались перекрыты заголовком.
            Box(modifier = Modifier.padding(padding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { pageIndex ->
                    TagsPaginatedListScreen(
                        pageIndex = pageIndex,
                        loadPage = { vm.loadPage(it).items },
                        onOpenVideo = { navigator.push(ScreenX_VideoPlayer(urlStart + it.href)) },
                    )
                }
            }
        }
    }

}
