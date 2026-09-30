package com.client.xvideos.x.screens.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.tags.atom.TagsHeader
import com.client.xvideos.x.screens.tags.molecule.TagsPaginatedListScreen
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
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

        val onBack: () -> Unit = remember(navigator) { { navigator.pop().let {} } }
        BackHandler(onBack = onBack)

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

        ScreenTagsContent(
            tag = tag,
            title0 = vm.screen.title0,
            title1 = vm.screen.title1,
            lastPage = vm.screen.lastPage,
            topCutout = topCutout,
            pagerState = pagerState,
            listStates = listStates,
            loadPage = loadPage,
            onOpenVideo = onOpenVideo,
            onPageChange = onPageChange
        )
    }
}

@Composable
fun ScreenTagsContent(
    tag: String,
    title0: String,
    title1: String,
    lastPage: Int,
    topCutout: Dp,
    pagerState: PagerState,
    listStates: MutableMap<Int, LazyListState>,
    loadPage: suspend (Int) -> List<ItemsX>,
    onOpenVideo: (ItemsX) -> Unit,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderHeader: @Composable () -> Unit = remember(tag, title0, title1, topCutout) {
        {
            TagsHeader(
                tag = tag,
                title0 = title0,
                title1 = title1,
                topCutout = topCutout
            )
        }
    }

    val bottomBarContent: @Composable () -> Unit = remember(pagerState.currentPage, onPageChange, lastPage) {
        {
            BottomListDashBoardNavigationButtons2(
                value = pagerState.currentPage,
                onChange = onPageChange,
                max = lastPage,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.L.grey6,
        bottomBar = bottomBarContent,
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
                key = { pageIndex -> pageIndex }
            ) { pageIndex ->
                TagsPaginatedListScreen(
                    pageIndex = pageIndex,
                    loadPage = loadPage,
                    onOpenVideo = onOpenVideo,
                    listState = listStates.getOrPut(pageIndex) { LazyListState() },
                    header = renderHeader
                )
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
