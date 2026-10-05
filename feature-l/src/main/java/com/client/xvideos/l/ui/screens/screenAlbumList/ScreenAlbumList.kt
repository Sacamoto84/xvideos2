package com.client.xvideos.l.ui.screens.screenAlbumList

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.TopLoadingBar
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumListFilter as LAlbumListFilter
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar.AlbumListBottomBar
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.AlbumListFilterOverlay
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.AlbumListPageGrid
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.AlbumListPageWithStatus
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ExperimentalZoomableApi

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
@Composable
private fun Screen.ScreenAlbumListContent(
    initialFilter: LAlbumListFilter?,
    title: String = "",
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.currentOrThrow
    val vm = getScreenModel<ScreenLAlbumListSM, ScreenLAlbumListSM.Factory> { factory ->
        factory.create(initialFilter)
    }
    val bigList = vm.bigList
    val info by vm.info.collectAsStateWithLifecycle()
    val currentFilter by vm.filter.collectAsStateWithLifecycle()
    val requestsInFlight by vm.requestsInFlight.collectAsStateWithLifecycle()
    val filterGCount by vm.filterGenreStateCount.collectAsStateWithLifecycle()
    val filterTagsCount by vm.filterTaggedStateCount.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var totalPages by remember { mutableIntStateOf(1) }

    var showFilterDialog by remember { mutableStateOf(false) }
    val onFilterClose: () -> Unit = remember { { showFilterDialog = false } }

    BackHandler(enabled = showFilterDialog, onBack = onFilterClose)

    val topInset = getTopInsetDp()

    LaunchedEffect(info) { totalPages = info?.totalPages ?: 1 }

    // Ключ — и число страниц: оно приходит с первым ответом и сбрасывается при
    // смене фильтра. По одной смене страницы соседние не подгружались, пока
    // пользователь не листнёт: и при открытии, и после фильтра пейджер стоит на
    // нулевой странице.
    LaunchedEffect(vm.statePager.currentPage, totalPages) {
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

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (requestsInFlight > 0) {
                    TopLoadingBar()
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
        ) { padding ->

            HorizontalPager(
                vm.statePager,
                Modifier
                    .padding(bottom = padding.calculateBottomPadding())
                    .fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> "${key}_page_$page" }
            ) { page ->

                val pageState = bigList[page]
                val pageItems = pageState?.albumListImplInfoAndList?.items.orEmpty()

                AlbumListPageWithStatus(
                    status = pageState?.status,
                    isEmpty = pageItems.isEmpty(),
                    errorMessage = pageState?.errorMessage,
                    onRetry = remember(vm, page) { { vm.loadAlbumList(page) } },
                ) {
                    AlbumListPageGrid(
                        stateGrid = vm.stateGrid.getOrPut(page) { LazyGridState() },
                        pageItems = pageItems,
                        title = title,
                        topInset = topInset,
                        haptic = haptic,
                        onAlbumClick = onAlbumClick
                    )
                }
            }

        }

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

internal enum class AlbumListBackAction {
    DISMISS_FILTER,
    POP
}

internal fun resolveAlbumListBackAction(
    showFilterDialog: Boolean
): AlbumListBackAction {
    return if (showFilterDialog) AlbumListBackAction.DISMISS_FILTER else AlbumListBackAction.POP
}

@Preview
@Composable
private fun ScreenAlbumListPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
