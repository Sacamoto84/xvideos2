package com.client.xvideos.x.screens.rubric

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.common.playerScreenFor
import com.client.xvideos.x.screens.common.rememberLocalVideoOf
import com.client.xvideos.x.screens.rubric.molecule.XRubricFilterBar
import com.client.xvideos.x.screens.rubric.molecule.XRubricHeader
import com.client.xvideos.x.screens.tags.model.TagCellActions
import com.client.xvideos.x.screens.tags.molecule.TagsPaginatedListScreen
import kotlinx.coroutines.launch

/**
 * Экран категории X из меню сайта: выдача `/c/<slug>-<id>` по страницам с
 * фильтрами. Ориентация на экран не влияет — ленту задаёт [id].
 */
class ScreenXRubric(val id: Int, val slug: String, val title: String) : Screen {

    override val key: ScreenKey = "ScreenXRubric:$id"

    @Composable
    override fun Content() {
        val vm = getScreenModel<ScreenXRubricSM, ScreenXRubricSM.Factory> { factory -> factory.create(id, slug) }
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val feed = vm.feed
        val filters = feed.filters
        val screen = feed.screen
        val openGroup = vm.openGroup

        // Другие фильтры — другая выдача: пейджер создаётся заново, с первой
        // страницы. Страница — из ScreenModel, как у экрана тега.
        val pagerState = key(filters) {
            rememberPagerState(initialPage = feed.currentPage) { feed.screen.lastPage.coerceAtLeast(1) }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page -> feed.currentPage = page }
        }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop().let {} } }
        BackHandler(onBack = onBack)

        val topCutout = getTopInsetDp()
        val onPageChange: (Int) -> Unit = remember(scope, pagerState) {
            { page -> scope.launch { pagerState.animateScrollToPage(page) } }
        }
        val loadPage: suspend (Int) -> List<ItemsX> = remember(feed) {
            { page -> feed.loadPage(page).items }
        }
        val localVideoOf = rememberLocalVideoOf(vm.saved.downloads)
        val cellActions = remember(navigator, vm, localVideoOf) {
            TagCellActions(
                onOpenVideo = { item -> navigator.push(playerScreenFor(item, vm.saved.downloads.localVideo(item.id))) },
                isFavorite = vm::isFavorite,
                onFavoriteAdd = vm::addFavorite,
                onFavoriteRemove = vm::removeFavorite,
                onDownload = vm::download,
                onSaveToGallery = vm::saveToGallery,
                localVideoOf = localVideoOf,
            )
        }
        val subtitle = screen.title1
        val header: @Composable () -> Unit = remember(subtitle, topCutout, filters, openGroup, vm) {
            {
                XRubricHeader(
                    title = title,
                    subtitle = subtitle,
                    topCutout = topCutout,
                    filterBar = {
                        XRubricFilterBar(
                            filters = filters,
                            openGroup = openGroup,
                            onToggleGroup = vm::toggleGroup,
                            onSelect = vm::selectFilter,
                        )
                    },
                )
            }
        }

        ScreenXRubricContent(
            header = header,
            lastPage = screen.lastPage,
            pageKeyPrefix = filters.pathPrefix,
            pagerState = pagerState,
            listPositions = feed.listPositions,
            shownPerRow = feed.shownPerRow,
            loadPage = loadPage,
            cachedItems = feed::cachedItems,
            cellActions = cellActions,
            onPageChange = onPageChange,
        )
    }
}

/**
 * Корневая компоновка экрана категории: пейджер страниц выдачи и нижняя лента страниц.
 *
 * @param pageKeyPrefix Часть ключа страницы пейджера от фильтров: со сменой фильтров
 *   страницы создаются заново, а не показывают ролики прежней выдачи.
 */
@Composable
fun ScreenXRubricContent(
    header: @Composable () -> Unit,
    lastPage: Int,
    pageKeyPrefix: String,
    pagerState: PagerState,
    listPositions: MutableMap<Int, LazyListPosition>,
    shownPerRow: MutableMap<Int, MutableIntState>,
    loadPage: suspend (Int) -> List<ItemsX>,
    cachedItems: (Int) -> List<ItemsX>?,
    cellActions: TagCellActions,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                key = { pageIndex -> "$pageKeyPrefix#$pageIndex" }
            ) { pageIndex ->
                TagsPaginatedListScreen(
                    pageIndex = pageIndex,
                    loadPage = loadPage,
                    initialItems = cachedItems(pageIndex),
                    actions = cellActions,
                    listState = remember(pageIndex) { listPositions.getOrPut(pageIndex) { LazyListPosition() } }
                        .rememberState(),
                    shownPerRow = remember(pageIndex) { shownPerRow.getOrPut(pageIndex) { mutableIntStateOf(0) } },
                    header = header
                )
            }
        }
    }
}
