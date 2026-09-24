package com.client.xvideos.x.screens.tags

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.client.xvideos.common.util.getTopInsetDp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import kotlinx.coroutines.launch

class ScreenTags(val tag: String) : Screen {

    override val key: ScreenKey = "ScreenTags:$tag"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenTagsViewModel, ScreenTagsViewModel.Factory> { factory -> factory.create(tag) }
        val navigator = LocalNavigator.currentOrThrow
        val job = rememberCoroutineScope()

        // Число страниц приходит с нулевой страницей; до её разбора пейджер
        // держит одну. pageCount читается лениво, поэтому рост с 1 до 149
        // пейджер подхватывает без пересоздания состояния.
        val pagerState = rememberPagerState(initialPage = 0) { vm.screen.lastPage.coerceAtLeast(1) }
        val listStates = remember { mutableStateMapOf<Int, LazyListState>() }

        BackHandler {
            navigator.pop()
        }

        val topCutout = getTopInsetDp()

        val onPageChange: (Int) -> Unit = remember(job, pagerState) {
            { page -> job.launch { pagerState.animateScrollToPage(page) } }
        }
        val loadPage: suspend (Int) -> List<ItemsX> = remember(vm) {
            { page -> vm.loadPage(page).items }
        }
        val onOpenVideo: (ItemsX) -> Unit = remember(navigator) {
            { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Theme.L.grey6,
            bottomBar = {
                // Без кнопки страны, в отличие от ленты раздела: адрес
                // /tags/<тег>/N от страны не зависит.
                BottomListDashBoardNavigationButtons2(
                    value = pagerState.currentPage,
                    onChange = onPageChange,
                    max = vm.screen.lastPage,
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { pageIndex ->
                    TagsPaginatedListScreen(
                        pageIndex = pageIndex,
                        loadPage = loadPage,
                        onOpenVideo = onOpenVideo,
                        listState = listStates.getOrPut(pageIndex) { LazyListState() },
                        header = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = topCutout + 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (vm.screen.title0.isNotBlank() || vm.screen.title1.isNotBlank()) {
                                    Row {
                                        if (vm.screen.title0.isNotBlank()) {
                                            Text(
                                                text = vm.screen.title0 + " ",
                                                color = Color(0xFFB0B0B0),
                                                fontSize = 12.sp,
                                            )
                                        }
                                        if (vm.screen.title1.isNotBlank()) {
                                            Text(
                                                text = vm.screen.title1,
                                                color = Color(0xFF787878),
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

}

internal enum class TagsBackAction {
    POP
}

internal fun resolveTagsBackAction(): TagsBackAction {
    return TagsBackAction.POP
}

