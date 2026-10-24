package com.client.xvideos.x.screens.dashboards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.feature.category.XCategoryState
import com.client.xvideos.x.feature.category.molecule.XCategorySwitch
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.dashboards.model.SavedTabListPositions
import com.client.xvideos.x.screens.dashboards.molecule.DashboardSecondaryControls
import com.client.xvideos.x.screens.dashboards.molecule.MAIN_TAB_SAVABLE
import com.client.xvideos.x.screens.dashboards.molecule.MAIN_TAB_SEARCH
import com.client.xvideos.x.screens.dashboards.molecule.SAVED_TAB_COUNT
import com.client.xvideos.x.screens.dashboards.molecule.SavedTabContent
import com.client.xvideos.x.screens.dashboards.vm.ScreenXDashBoardsScreenModel
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.search.ScreenXSearchSM
import com.client.xvideos.x.screens.search.ScreenXSearchTabContent
import com.client.xvideos.x.screens.search.SearchUiMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import com.client.xvideos.x.screens.common.rememberLocalVideoOf

private val mainTabs: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.Dashboard,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Search,
)

/**
 * Главный экран раздела X с двухуровневой нижней панелью в стиле R/L.
 */
class ScreenXDashBoards : Screen {

    override val key: ScreenKey = "ScreenXDashBoards"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenXDashBoardsScreenModel = getScreenModel()
        val searchVm: ScreenXSearchSM = getScreenModel()

        val searchUiMode by searchVm.uiMode.collectAsStateWithLifecycle()
        val searchPage by searchVm.currentPage.collectAsStateWithLifecycle()
        val searchMaxPages by searchVm.maxPages.collectAsStateWithLifecycle()
        val downloadPercent by vm.saved.downloads.percent.collectAsStateWithLifecycle()

        val onBack: () -> Unit = remember(vm, searchVm) {
            {
                if (vm.mainTab == MAIN_TAB_SEARCH) {
                    if (!searchVm.onBackPress()) {
                        vm.mainTab = 0
                    }
                } else {
                    vm.mainTab = 0
                }
            }
        }
        BackHandler(enabled = vm.mainTab != 0, onBack = onBack)

        val favoritesScreen = remember { ScreenFavorites() }
        val scope = rememberCoroutineScope()
        val pagerState = vm.pagerPosition.rememberState { 20000 }
        val savedPagerState = vm.savedPagerPosition.rememberState { SAVED_TAB_COUNT }

        // Другая категория — другая лента и выдача: лента встаёт на первую страницу,
        // открытый поиск перезапрашивается. Тот же пункт меню ничего не сбрасывает.
        val onCategorySelect: (XCategory) -> Unit = remember(searchVm, pagerState, scope) {
            { selected ->
                if (selected != XCategoryState.current) {
                    XCategoryState.select(selected)
                    scope.launch { pagerState.scrollToPage(0) }
                    searchVm.onCategoryChanged()
                }
            }
        }

        ScreenXDashBoardsContent(
            mainTab = vm.mainTab,
            savedPagerState = savedPagerState,
            savedListPositions = vm.savedListPositions,
            pagerState = pagerState,
            saved = vm.saved,
            searchVm = searchVm,
            searchUiMode = searchUiMode,
            searchPage = searchPage,
            searchMaxPages = searchMaxPages,
            downloadPercent = downloadPercent,
            favoritesScreen = favoritesScreen,
            navigator = navigator,
            onMainTabChange = { vm.mainTab = it },
            onSavedTabChange = { tab -> scope.launch { savedPagerState.scrollToPage(tab) } },
            onOpenVideoPlayer = { item -> vm.openVideoPlayer(item, navigator) },
            onIsFavorite = { itemId -> vm.isFavorite(itemId) },
            onFavoriteAdd = { item -> vm.addFavorite(item) },
            onFavoriteRemove = { item -> vm.removeFavorite(item) },
            onDownload = { item -> vm.download(item) },
            onSaveToGallery = { item -> vm.saveToGallery(item) },
            onLoadDashboardPage = vm::loadDashboardPage,
            cachedDashboardPage = vm::cachedDashboardPage,
            category = XCategoryState.current,
            onCategorySelect = onCategorySelect,
        )
    }
}

/**
 * Корневая функция компоновки экрана дашбордов.
 */
@Suppress("LongParameterList")
@Composable
fun ScreenXDashBoardsContent(
    mainTab: Int,
    savedPagerState: PagerState,
    savedListPositions: SavedTabListPositions,
    pagerState: PagerState,
    saved: SavedX,
    searchVm: ScreenXSearchSM,
    searchUiMode: SearchUiMode,
    searchPage: Int,
    searchMaxPages: Int,
    downloadPercent: Float,
    favoritesScreen: ScreenFavorites,
    navigator: Navigator,
    onMainTabChange: (Int) -> Unit,
    onSavedTabChange: (Int) -> Unit,
    onOpenVideoPlayer: (ItemsX) -> Unit,
    onIsFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    onLoadDashboardPage: suspend (Int) -> ImmutableList<ItemsX>,
    cachedDashboardPage: (Int) -> ImmutableList<ItemsX>?,
    category: XCategory,
    onCategorySelect: (XCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onDashboardPageChange: suspend (Int) -> Unit = remember(pagerState) {
        { page -> pagerState.scrollToPage(page.coerceAtLeast(0)) }
    }
    val onSearchPageChange: (Int) -> Unit = remember(searchVm) {
        { page -> searchVm.onPageChange(page) }
    }
    val onOpenChannel: (String, Boolean) -> Unit = remember(navigator) {
        { slug, isModel -> navigator.push(ScreenX_Channel(slug = slug, isModel = isModel)) }
    }
    // Сохранённые ролики в ленте: превью и плеер — из скачанного файла.
    val localVideoOf = rememberLocalVideoOf(saved.downloads)

    val snapAnimationSpec = remember {
        spring(
            stiffness = 600f,
            visibilityThreshold = Int.VisibilityThreshold.toFloat()
        )
    }
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapPositionalThreshold = 0.15f,
        snapAnimationSpec = snapAnimationSpec
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFF333333))

                DashboardSecondaryControls(
                    mainTab = mainTab,
                    savedTab = savedPagerState.currentPage,
                    searchUiMode = searchUiMode,
                    searchPage = searchPage,
                    searchMaxPages = searchMaxPages,
                    pagerCurrentPage = pagerState.currentPage,
                    pagerPageCount = pagerState.pageCount,
                    onSavedTabChange = onSavedTabChange,
                    onDashboardPageChange = onDashboardPageChange,
                    onSearchPageChange = onSearchPageChange
                )

                Row(
                    modifier = Modifier.background(Theme.tabLevel0),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TabRow(
                            titlesIcon = mainTabs,
                            value = mainTab,
                            onChangeState = onMainTabChange,
                            containerColor = Theme.tabLevel0,
                        )
                    }
                    XCategorySwitch(current = category, onSelect = onCategorySelect)
                }

                DownloadIndicator(downloadPercent)
            }
        },
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            when (mainTab) {
                MAIN_TAB_SAVABLE -> SavedTabContent(
                    pagerState = savedPagerState,
                    favoritesScreen = favoritesScreen,
                    listPositions = savedListPositions,
                    saved = saved
                )
                MAIN_TAB_SEARCH -> ScreenXSearchTabContent(
                    vm = searchVm,
                    onOpenVideoPlayer = onOpenVideoPlayer,
                    onOpenChannel = onOpenChannel
                )
                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { pageIndex -> pageIndex },
                    flingBehavior = pagerFlingBehavior
                ) { pageIndex ->
                    DashboardsPaginatedListScreen(
                        pageIndex = pageIndex,
                        category = category,
                        localVideoOf = localVideoOf,
                        openVideoPlayer = onOpenVideoPlayer,
                        isFavorite = onIsFavorite,
                        onFavoriteAdd = onFavoriteAdd,
                        onFavoriteRemove = onFavoriteRemove,
                        onDownload = onDownload,
                        loadPage = onLoadDashboardPage,
                        onSaveToGallery = onSaveToGallery,
                        cachedPage = cachedDashboardPage,
                    )
                }
            }
        }
    }
}
