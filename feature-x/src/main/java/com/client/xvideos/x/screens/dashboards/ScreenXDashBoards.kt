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
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.history.ScreenXHistory
import com.client.xvideos.x.screens.saved.X_SavedContent
import com.client.xvideos.x.model.ItemsX

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
        // При нажатии «Назад» во вторичных табах сохраненного возвращаемся в ленту дашбордов
        val onBack: () -> Unit = remember(vm) { { vm.mainTab = 0 } }
        BackHandler(enabled = vm.mainTab != 0, onBack = onBack)

        // Стабильный экземпляр «Избранного» для инлайн-рендера (как object-табы saved в R/L).
        val favoritesScreen = remember { ScreenFavorites() }

        // Прогресс загрузки для зелёного индикатора снизу (как в R).
        val downloadPercent by vm.saved.downloads.percent.collectAsStateWithLifecycle()

        val onSavedTabChange: (Int) -> Unit = remember(vm) { { newTab -> vm.savedTab = newTab } }
        val onMainTabChange: (Int) -> Unit = remember(vm) { { newTab -> vm.mainTab = newTab } }
        val onDashboardPageChange: suspend (Int) -> Unit = remember(vm) { { page -> vm.pagerState.scrollToPage(page.coerceAtLeast(0)) } }
        val onOpenVideoPlayer: (ItemsX) -> Unit = remember(vm, navigator) { { item -> vm.openVideoPlayer(item, navigator) } }
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
                    when (vm.mainTab) {
                        SAVABLE -> TabRow(
                            titlesIcon = savedTabs,
                            value = vm.savedTab,
                            onChangeState = onSavedTabChange,
                            containerColor = Theme.tabLevel1,
                        )
                        else -> DashboardControlsRow(
                            isCurrentPage = vm.pagerState.currentPage,
                            isMax = vm.pagerState.pageCount,
                            onChange = onDashboardPageChange
                        )
                    }

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
                    SAVABLE -> when (vm.savedTab) {
                        SAVED_FAVORITES -> favoritesScreen.Content()
                        SAVED_DOWNLOADS -> X_SavedContent(vm.saved)
                        SAVED_HISTORY -> ScreenXHistory(vm.saved)
                        else -> favoritesScreen.Content()
                    }
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

    companion object {
        private const val SAVABLE = 1

        // Под-табы раздела Savable.
        private const val SAVED_FAVORITES = 0
        private const val SAVED_DOWNLOADS = 1
        private const val SAVED_HISTORY = 2

        /** Иконки главного таб-ряда: дашборды + сохранённое. */
        // persistentListOf, а не listOf: обычный List для Compose нестабилен,
        // и TabRow перекомпоновывался чаще, чем нужно.
        private val mainTabs: ImmutableList<ImageVector> = persistentListOf(
            Icons.Outlined.Dashboard,
            Icons.Outlined.BookmarkBorder,
        )

        /** Под-табы раздела Savable: «Избранное» + «Сохранённое» + «История». */
        private val savedTabs: ImmutableList<ImageVector> = persistentListOf(
            Icons.Outlined.FavoriteBorder,
            Icons.Outlined.Save,
            Icons.Outlined.History,
        )
    }
}


