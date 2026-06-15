package com.client.xvideos.x.screens.dashboards

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.ui.screens.TabRow
import com.client.xvideos.x.screens.dashboards.bottomBar.DashboardControlsRow
import com.client.xvideos.x.screens.dashboards.vm.ScreenXDashBoardsScreenModel
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.saved.X_SavedContent
import com.client.xvideos.r.common.downloader.ui.DownloadIndicator

/**
 * Главный экран раздела X с двухуровневой нижней панелью в стиле R/L.
 *
 * - Главный таб-ряд снизу (общий компонент [TabRow], уровень [Theme.tabLevel0]):
 *   `Dashboards` и `Savable`.
 * - Второй ряд над ним зависит от выбранного таба:
 *     - `Dashboards` → [DashboardControlsRow]: кнопка страны + выбор текущей страницы;
 *     - `Savable`    → под-[TabRow] (уровень [Theme.tabLevel1]) с под-вкладками
 *                      `Favorites` и `Сохранённое`.
 * - Самым нижним элементом панели идёт зелёный [DownloadIndicator] (как в R-root).
 * - Тело переключается между пейджером дашбордов, «Избранным» и «Сохранённым».
 */
class ScreenXDashBoards : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenXDashBoardsScreenModel = getScreenModel()

        // Стабильный экземпляр «Избранного» для инлайн-рендера (как object-табы saved в R/L).
        val favoritesScreen = remember { ScreenFavorites() }

        // Прогресс загрузки для зелёного индикатора снизу (как в R).
        val downloadPercent by vm.saved.downloads.percent.collectAsStateWithLifecycle()

        Scaffold(
            bottomBar = {
                Column {
                    HorizontalDivider(color = Color(0xFF333333))

                    // Второй ряд — зависит от выбранного главного таба.
                    when (vm.mainTab) {
                        SAVABLE -> TabRow(
                            titlesIcon = savedTabs,
                            value = vm.savedTab,
                            onChangeState = { vm.savedTab = it },
                            containerColor = Theme.tabLevel1,
                        )
                        else -> DashboardControlsRow(
                            isCurrentPage = vm.pagerState.currentPage,
                            isMax = vm.pagerState.pageCount,
                            onChange = { vm.pagerState.scrollToPage(it.coerceAtLeast(0)) }
                        )
                    }

                    // Главный таб-ряд (R/L-стиль).
                    TabRow(
                        titlesIcon = mainTabs,
                        value = vm.mainTab,
                        onChangeState = { vm.mainTab = it },
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
                        SAVED_DOWNLOADS -> X_SavedContent(vm.saved)
                        else -> favoritesScreen.Content()
                    }
                    else -> HorizontalPager(
                        state = vm.pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = vm.pagerState,
                            snapPositionalThreshold = 0.15f,
                            snapAnimationSpec = spring(
                                stiffness = 600f,
                                visibilityThreshold = Int.VisibilityThreshold.toFloat()
                            )
                        )
                    ) { pageIndex ->
                        DashboardsPaginatedListScreen(
                            pageIndex,
                            openVideoPlayer = { vm.openVideoPlayer(it, navigator) },
                            isFavorite = { vm.isFavorite(it) },
                            onFavoriteAdd = { vm.addFavorite(it) },
                            onFavoriteRemove = { vm.removeFavorite(it) },
                            onDownload = { vm.download(it) },
                            onSaveToGallery = { vm.saveToGallery(it) },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val SAVABLE = 1

        // Под-табы раздела Savable.
        private const val SAVED_DOWNLOADS = 1

        /** Иконки главного таб-ряда: дашборды + сохранённое. */
        private val mainTabs: List<ImageVector> = listOf(
            Icons.Outlined.Dashboard,
            Icons.Outlined.BookmarkBorder,
        )

        /** Под-табы раздела Savable: «Избранное» + «Сохранённое». */
        private val savedTabs: List<ImageVector> = listOf(
            Icons.Outlined.FavoriteBorder,
            Icons.Outlined.Save,
        )
    }
}


