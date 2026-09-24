package com.client.xvideos.l.ui.screens.screenAlbumList

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar.AlbumListBottomBar
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.AlbumListFilter
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.model.AlbumListFilter as LAlbumListFilter


/**
 * Пейджер, у которого число страниц приходит извне и меняется по ходу загрузки.
 *
 * `Saver` здесь был, но не использовался ни разу: состояние живёт в
 * `ScreenAlbumListSM` и переживает пересоздание экрана вместе с ним. Хуже, что
 * при восстановлении он подставлял `updatedPageCount = { it[2] as Int }` —
 * лямбду-константу, — и число страниц замирало на сохранённом. Удалён, чтобы
 * его не подключили однажды по имени.
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


object L_ScreenAlbumList : Screen {

    private fun readResolve(): Any = L_ScreenAlbumList

    override val key: ScreenKey = "L_ScreenAlbumList"

    fun create(filter: LAlbumListFilter?, title: String = ""): Screen = ScreenLAlbumList(filter, title)

    @Composable
    override fun Content() {
        ScreenAlbumListContent(initialFilter = null)
    }
}

private class ScreenLAlbumList(
    private val initialFilter: LAlbumListFilter?,
    private val title: String
) : Screen {
    override val key: ScreenKey = "ScreenLAlbumList:${initialFilter?.hashCode() ?: 0}:$title"

    @Composable
    override fun Content() {
        ScreenAlbumListContent(initialFilter = initialFilter, title = title)
    }
}

@OptIn(ExperimentalZoomableApi::class)
@Suppress("LongMethod")
@Composable
private fun Screen.ScreenAlbumListContent(
    initialFilter: LAlbumListFilter?,
    title: String = ""
)
{
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenLAlbumListSM, ScreenLAlbumListSM.Factory> { factory ->
            factory.create(initialFilter)
        }
        val bigList = vm.bigList
        val info by vm.info.collectAsStateWithLifecycle()
        val currentFilter by vm.filter.collectAsStateWithLifecycle()
        val isRequest by vm.isRequest.collectAsStateWithLifecycle()
        val filterGCount by vm.filterGenreStateCount.collectAsStateWithLifecycle()
        val filterTagsCount by vm.filterTaggedStateCount.collectAsStateWithLifecycle()
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        var totalPages by remember { mutableIntStateOf(1) }

        // ✅ Состояние для диалога
        var showFilterDialog by remember { mutableStateOf(false) }

        BackHandler(enabled = showFilterDialog) {
            showFilterDialog = false
        }

        val topInset = getTopInsetDp()

        LaunchedEffect(info) { totalPages = info?.totalPages ?: 1 }

        LaunchedEffect(vm.statePager.currentPage) {
            vm.statePager.pageCountState.value = { totalPages }
            vm.savedPagerPage = vm.statePager.currentPage
            val currentPage = vm.statePager.currentPage
            val pagesToLoad = setOf(
                maxOf(0, currentPage - 1),
                currentPage,
                minOf(vm.statePager.pageCount - 1, currentPage + 1),
                minOf(vm.statePager.pageCount - 1, currentPage + 2)
            )
            pagesToLoad.forEach { page -> vm.loadAlbumList(page) }
        }

        val onClickVisibleFilter: () -> Unit = remember(haptic) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                showFilterDialog = true
            }
        }
        val onPageChange: (Int) -> Unit = remember(vm, scope, haptic) {
            { page ->
                scope.launch { vm.statePager.scrollToPage(page) }
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                vm.loadAlbumList(page)
            }
        }
        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId -> navigator.push(ScreenLAlbum(albumId)) }
        }
        val onFilterClose: () -> Unit = remember { { showFilterDialog = false } }
        val onFilterApply: (LAlbumListFilter) -> Unit = remember(vm) {
            { newFilter ->
                vm.screenModelScope.launch {
                    vm.stateGrid.clear()
                    vm.statePager.scrollToPage(0)
                    vm.filterUpdate(newFilter)
                    vm.loadInitialData()
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (isRequest) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xff0c94ff)),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                },
                bottomBar = {
                    AlbumListBottomBar(
                        onClickVisibleFilter = onClickVisibleFilter,
                        currentPage = vm.statePager.currentPage,
                        totalPages = info?.totalPages ?: 1,
                        onChange = onPageChange
                    )
                },
                containerColor = Theme.background
            )
            { padding ->

                HorizontalPager(
                    vm.statePager,
                    Modifier
                        .padding(bottom = padding.calculateBottomPadding())
                        .fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { page -> "${key}_page_$page" }
                ) { page ->

                    val pageItems = bigList[page]?.albumListImplInfoAndList?.items.orEmpty()
                    val stateGrid = vm.stateGrid.getOrPut(page) { LazyGridState() }

                    AlbumListPageGrid(
                        stateGrid = stateGrid,
                        pageItems = pageItems,
                        title = title,
                        topInset = topInset,
                        haptic = haptic,
                        onAlbumClick = onAlbumClick
                    )

                    val status = vm.bigList[page]?.status
                    if ((status == StatusAlbumList.DOWNLOADING) && (pageItems.isEmpty())){
                        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ) { CircularProgressIndicator() }
                    }

                } //HorizontalPager(

            } //Scaffold

            AlbumListFilterOverlay(
                visible = showFilterDialog,
                filter = currentFilter,
                filterGCount = filterGCount,
                filterTagsCount = filterTagsCount,
                onClose = onFilterClose,
                onApply = onFilterApply
            )
        }
    }

@Composable
private fun AlbumListPageGrid(
    stateGrid: LazyGridState,
    pageItems: List<Album>,
    title: String,
    topInset: Dp,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onAlbumClick: (Long) -> Unit
) {
    LazyVerticalGridScrollbar(
        state = stateGrid,
        settings = ScrollbarSettings.Default.copy(
            thumbUnselectedColor = Color(0xFFA3A3A3),
            thumbSelectedColor = Color(0xFFB3B3B3),
            thumbThickness = 3.dp,
            scrollbarPadding = 0.dp,
            alwaysShowScrollbar = false
        )
    ) {
        LazyVerticalGrid(state = stateGrid, modifier = Modifier.fillMaxSize(), columns = GridCells.Fixed(2)) {
            item(key = "dummy", span = { GridItemSpan(maxLineSpan) }) {
                if (title.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Theme.L.red)
                            .padding(top = topInset)
                            .height(44.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontFamily = Theme.L.fontFamilyKarla,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (topInset > 0.dp) {
                    Spacer(Modifier.height(topInset))
                }
            }

            items(
                items = pageItems,
                key = { it.id }
            ) { item ->
                AlbumGridItem(
                    item = item,
                    haptic = haptic,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
}

@Composable
private fun AlbumGridItem(
    item: Album,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onAlbumClick: (Long) -> Unit
) {
    val onClick: () -> Unit = remember(item.id, haptic, onAlbumClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            val id = item.id.toLongOrNull()
            if (id != null) {
                onAlbumClick(id)
            }
        }
    }
    Box(
        Modifier.padding(vertical = 2.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        AlbumListItem(
            title = item.title,
            coverUrl = item.cover?.url.orEmpty(),
            numberOfAnimatedPictures = item.numberOfAnimatedPictures,
            numberOfPictures = item.numberOfPictures,
            onClick = onClick
        )
    }
}

@Composable
private fun AlbumListFilterOverlay(
    visible: Boolean,
    filter: LAlbumListFilter,
    filterGCount: List<AlbumListFilterGenreCountResponse>?,
    filterTagsCount: List<AlbumListFilterGenreCountResponse>?,
    onClose: () -> Unit,
    onApply: (LAlbumListFilter) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClose)
            )

            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = screenHeight * 0.9f)
                    .wrapContentHeight()
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF171717),
                shadowElevation = 8.dp
            ) {
                AlbumListFilter(
                    filter = filter,
                    filterGCount = filterGCount,
                    filterTagsCount = filterTagsCount,
                    onClose = onClose,
                    onFilterApply = onApply
                )
            }
        }
    }
}

internal enum class AlbumListBackAction {
    DISMISS_FILTER,
    POP
}

internal fun resolveAlbumListBackAction(
    showFilterDialog: Boolean
): AlbumListBackAction {
    return if (showFilterDialog) AlbumListBackAction.DISMISS_FILTER else AlbumListBackAction.POP
}

