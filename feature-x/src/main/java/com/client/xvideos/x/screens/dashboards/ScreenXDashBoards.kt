package com.client.xvideos.x.screens.dashboards

import com.client.xvideos.common.theme.Theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.ui.TabRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.client.xvideos.x.screens.dashboards.bottomBar.DashboardControlsRow
import com.client.xvideos.x.screens.dashboards.vm.ScreenXDashBoardsScreenModel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tv
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.history.ScreenXHistory
import com.client.xvideos.x.screens.saved.X_SavedContent
import com.client.xvideos.x.screens.subscriptions.X_SubscriptionsContent
import com.client.xvideos.x.model.ItemsX
import androidx.compose.material.icons.outlined.Search
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.search.ScreenXSearchSM
import com.client.xvideos.x.screens.search.SearchUiMode
import com.client.xvideos.x.screens.search.X_SearchContent

/**
 * Главный экран раздела X с двухуровневой нижней панелью в стиле R/L.
 *
 * Нижняя панель (bottomBar у Scaffold) содержит:
 * - Разделитель;
 * - Контекстный ряд (зависит от выбранного главного таба):
 *     - `Dashboards` → [DashboardControlsRow]: кнопка страны + выбор текущей страницы;
 *     - `Savable`    → под-[TabRow] (уровень [Theme.tabLevel1]) с под-вкладками
 *                      `Favorites` и `Сохранённое`.
 * - Самым нижним элементом панели идёт зелёный [DownloadIndicator] (как в R-root).
 * - Тело переключается между пейджером дашбордов, «Избранным» и «Сохранённым».
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

        // При нажатии «Назад» во вторичных табах сохраненного/поиска возвращаемся в ленту дашбордов
        val onBack: () -> Unit = remember(vm, searchVm) {
            {
                if (vm.mainTab == SEARCH) {
                    if (!searchVm.onBackPress()) {
                        vm.mainTab = 0
                    }
                } else {
                    vm.mainTab = 0
                }
            }
        }
        BackHandler(enabled = vm.mainTab != 0, onBack = onBack)

        // Стабильный экземпляр «Избранного» для инлайн-рендера (как object-табы saved в R/L).
        val favoritesScreen = remember { ScreenFavorites() }

        // Прогресс загрузки для зелёного индикатора снизу (как в R).
        val downloadPercent by vm.saved.downloads.percent.collectAsStateWithLifecycle()

        val onSavedTabChange: (Int) -> Unit = remember(vm) { { newTab -> vm.savedTab = newTab } }
        val onMainTabChange: (Int) -> Unit = remember(vm) { { newTab -> vm.mainTab = newTab } }
        val onDashboardPageChange: suspend (Int) -> Unit = remember(vm) { { page -> vm.pagerState.scrollToPage(page.coerceAtLeast(0)) } }
        val onSearchPageChange: (Int) -> Unit = remember(searchVm) { { page -> searchVm.onPageChange(page) } }
        val onOpenVideoPlayer: (ItemsX) -> Unit = remember(vm, navigator) { { item -> vm.openVideoPlayer(item, navigator) } }
        val onOpenChannel: (String, Boolean) -> Unit = remember(navigator) {
            { slug, isModel -> navigator.push(ScreenX_Channel(slug = slug, isModel = isModel)) }
        }
        val onIsFavorite: (Long) -> Boolean = remember(vm) { { itemId -> vm.isFavorite(itemId) } }
        val onFavoriteAdd: (ItemsX) -> Unit = remember(vm) { { item -> vm.addFavorite(item) } }
        val onFavoriteRemove: (ItemsX) -> Unit = remember(vm) { { item -> vm.removeFavorite(item) } }
        val onDownload: (ItemsX) -> Unit = remember(vm) { { item -> vm.download(item) } }
        val onSaveToGallery: (ItemsX) -> Unit = remember(vm) { { item -> vm.saveToGallery(item) } }

        val snapAnimationSpec = remember {
            spring(
                stiffness = 600f,
                visibilityThreshold = Int.VisibilityThreshold.toFloat()
            )
        }
        val pagerFlingBehavior = PagerDefaults.flingBehavior(
            state = vm.pagerState,
            snapPositionalThreshold = 0.15f,
            snapAnimationSpec = snapAnimationSpec
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column {
                    HorizontalDivider(color = Color(0xFF333333))

                    // Второй ряд — зависит от выбранного главного таба.
                    DashboardSecondaryControls(
                        mainTab = vm.mainTab,
                        savedTab = vm.savedTab,
                        searchUiMode = searchUiMode,
                        searchPage = searchPage,
                        searchMaxPages = searchMaxPages,
                        pagerCurrentPage = vm.pagerState.currentPage,
                        pagerPageCount = vm.pagerState.pageCount,
                        onSavedTabChange = onSavedTabChange,
                        onDashboardPageChange = onDashboardPageChange,
                        onSearchPageChange = onSearchPageChange
                    )

                    // Главный таб-ряд (R/L-стиль).
                    TabRow(
                        titlesIcon = mainTabs,
                        value = vm.mainTab,
                        onChangeState = onMainTabChange,
                        containerColor = Theme.tabLevel0,
                    )

                    // Зелёный индикатор загрузки (как в R-root).
                    DownloadIndicator(downloadPercent)
                }
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.background,
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .fillMaxSize()
            ) {
                when (vm.mainTab) {
                    SAVABLE -> SavedTabContent(
                        savedTab = vm.savedTab,
                        favoritesScreen = favoritesScreen,
                        saved = vm.saved
                    )
                    SEARCH -> X_SearchContent(
                        vm = searchVm,
                        onOpenVideoPlayer = onOpenVideoPlayer,
                        onOpenChannel = onOpenChannel
                    )
                    else -> HorizontalPager(
                        state = vm.pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        key = { pageIndex -> pageIndex },
                        flingBehavior = pagerFlingBehavior
                    ) { pageIndex ->
                        DashboardsPaginatedListScreen(
                            pageIndex = pageIndex,
                            openVideoPlayer = onOpenVideoPlayer,
                            isFavorite = onIsFavorite,
                            onFavoriteAdd = onFavoriteAdd,
                            onFavoriteRemove = onFavoriteRemove,
                            onDownload = onDownload,
                            onSaveToGallery = onSaveToGallery,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DashboardSecondaryControls(
        mainTab: Int,
        savedTab: Int,
        searchUiMode: SearchUiMode,
        searchPage: Int,
        searchMaxPages: Int,
        pagerCurrentPage: Int,
        pagerPageCount: Int,
        onSavedTabChange: (Int) -> Unit,
        onDashboardPageChange: suspend (Int) -> Unit,
        onSearchPageChange: (Int) -> Unit
    ) {
        when (mainTab) {
            SAVABLE -> TabRow(
                titlesIcon = savedTabs,
                value = savedTab,
                onChangeState = onSavedTabChange,
                containerColor = Theme.tabLevel1,
            )
            SEARCH -> {
                if (searchUiMode == SearchUiMode.RESULTS) {
                    BottomListDashBoardNavigationButtons2(
                        value = searchPage,
                        onChange = onSearchPageChange,
                        max = searchMaxPages
                    )
                }
            }
            else -> DashboardControlsRow(
                isCurrentPage = pagerCurrentPage,
                isMax = pagerPageCount,
                onChange = onDashboardPageChange
            )
        }
    }

    @Composable
    private fun SavedTabContent(
        savedTab: Int,
        favoritesScreen: ScreenFavorites,
        saved: SavedX
    ) {
        when (savedTab) {
            SAVED_FAVORITES -> favoritesScreen.Content()
            SAVED_DOWNLOADS -> X_SavedContent(saved)
            SAVED_HISTORY -> ScreenXHistory(saved)
            SAVED_CHANNELS -> X_SubscriptionsContent(saved = saved, isModel = false)
            SAVED_MODELS -> X_SubscriptionsContent(saved = saved, isModel = true)
            else -> favoritesScreen.Content()
        }
    }

    companion object {
        private const val SAVABLE = 1
        private const val SEARCH = 2

        // Под-табы раздела Savable.
        private const val SAVED_FAVORITES = 0
        private const val SAVED_DOWNLOADS = 1
        private const val SAVED_HISTORY = 2
        private const val SAVED_CHANNELS = 3
        private const val SAVED_MODELS = 4

        /** Иконки главного таб-ряда: дашборды + сохранённое + поиск. */
        // persistentListOf, а не listOf: обычный List для Compose нестабилен,
        // и TabRow перекомпоновывался чаще, чем нужно.
        private val mainTabs: ImmutableList<ImageVector> = persistentListOf(
            Icons.Outlined.Dashboard,
            Icons.Outlined.BookmarkBorder,
            Icons.Outlined.Search,
        )

        /** Под-табы раздела Savable: «Избранное» + «Сохранённое» + «История» + «Каналы» + «Актрисы». */
        private val savedTabs: ImmutableList<ImageVector> = persistentListOf(
            Icons.Outlined.FavoriteBorder,
            Icons.Outlined.Save,
            Icons.Outlined.History,
            Icons.Outlined.Tv,
            Icons.Outlined.Person,
        )
    }
}


