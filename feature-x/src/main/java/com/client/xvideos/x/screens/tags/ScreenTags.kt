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
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import kotlinx.coroutines.launch

private val ZERO_INSETS = WindowInsets(0, 0, 0, 0)
private val SUBTITLE_PRIMARY_COLOR = Color(0xFFB0B0B0)
private val SUBTITLE_SECONDARY_COLOR = Color(0xFF787878)
private val HEADER_TOP_PADDING_EXTRA = 8.dp
private val HEADER_HORIZONTAL_PADDING = 16.dp
private val HEADER_BOTTOM_PADDING = 8.dp
private val TAG_TITLE_FONT_SIZE = 20.sp
private val TAG_SUBTITLE_FONT_SIZE = 12.sp
private const val BEYOND_VIEWPORT_PAGE_COUNT = 1

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

        TagsContent(
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
fun TagsContent(
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = ZERO_INSETS,
        containerColor = Theme.L.grey6,
        bottomBar = {
            // Без кнопки страны, в отличие от ленты раздела: адрес
            // /tags/<тег>/N от страны не зависит.
            BottomListDashBoardNavigationButtons2(
                value = pagerState.currentPage,
                onChange = onPageChange,
                max = lastPage,
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
                beyondViewportPageCount = BEYOND_VIEWPORT_PAGE_COUNT,
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

@Composable
private fun TagsHeader(
    tag: String,
    title0: String,
    title1: String,
    topCutout: Dp,
    modifier: Modifier = Modifier
) {
    val hasTitle0 = remember(title0) { title0.isNotBlank() }
    val hasTitle1 = remember(title1) { title1.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = topCutout + HEADER_TOP_PADDING_EXTRA,
                start = HEADER_HORIZONTAL_PADDING,
                end = HEADER_HORIZONTAL_PADDING,
                bottom = HEADER_BOTTOM_PADDING
            )
    ) {
        Text(
            text = tag,
            color = Color.White,
            fontSize = TAG_TITLE_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasTitle0 || hasTitle1) {
            Row {
                if (hasTitle0) {
                    Text(
                        text = "$title0 ",
                        color = SUBTITLE_PRIMARY_COLOR,
                        fontSize = TAG_SUBTITLE_FONT_SIZE,
                    )
                }
                if (hasTitle1) {
                    Text(
                        text = title1,
                        color = SUBTITLE_SECONDARY_COLOR,
                        fontSize = TAG_SUBTITLE_FONT_SIZE,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun TagsHeaderPreview() {
    XvideosTheme(darkTheme = true) {
        TagsHeader(
            tag = "vr",
            title0 = "Virtual Reality Videos",
            title1 = "12,450 results",
            topCutout = 24.dp
        )
    }
}

internal enum class TagsBackAction {
    POP
}

internal fun resolveTagsBackAction(): TagsBackAction {
    return TagsBackAction.POP
}
