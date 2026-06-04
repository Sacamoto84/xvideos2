package com.client.xvideos.screens.dashboards

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.client.xvideos.r.common.ThemeRed
import com.client.xvideos.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.feature.country.ComposeCountry
import com.client.xvideos.x.screens.dashboards.DashboardsPaginatedListScreen
import com.client.xvideos.x.screens.dashboards.vm.ScreenXDashBoardsScreenModel
import com.client.xvideos.x.screens.favorites.ScreenFavorites
import com.client.xvideos.x.screens.saved.X_SavedContent
import com.redgifs.common.downloader.ui.DownloadIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Главный экран раздела X с двухуровневой нижней панелью в стиле R/L.
 *
 * - Главный таб-ряд снизу (общий компонент [TabRow], уровень [ThemeRed.colorTabLevel0]):
 *   `Dashboards` и `Savable`.
 * - Второй ряд над ним зависит от выбранного таба:
 *     - `Dashboards` → [DashboardControlsRow]: кнопка страны + выбор текущей страницы;
 *     - `Savable`    → под-[TabRow] (уровень [ThemeRed.colorTabLevel1]) с под-вкладками
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
                            containerColor = ThemeRed.colorTabLevel1,
                        )
                        else -> DashboardControlsRow(vm)
                    }

                    // Главный таб-ряд (R/L-стиль).
                    TabRow(
                        titlesIcon = mainTabs,
                        value = vm.mainTab,
                        onChangeState = { vm.mainTab = it },
                        containerColor = ThemeRed.colorTabLevel0,
                    )

                    // Зелёный индикатор загрузки (как в R-root).
                    DownloadIndicator(downloadPercent)
                }
            },
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
        ) { innerPadding ->

            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                        DashboardsPaginatedListScreen(pageIndex, vm)
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

/**
 * Второй ряд дашборда: кнопка страны + выбор текущей страницы.
 * Объединяет в одну строку бывший `TopBarDashboard` (страна) и ряд навигации страниц.
 */
@Composable
private fun DashboardControlsRow(vm: ScreenXDashBoardsScreenModel) {
    val job = rememberCoroutineScope()
    Row(modifier = Modifier.fillMaxWidth()) {
        ComposeCountry()
        Box(modifier = Modifier.weight(1f)) {
            BottomListDashBoardNavigationButtons2(
                value = vm.pagerState.currentPage,
                onChange = { job.launch(Dispatchers.Main) { vm.pagerState.scrollToPage(it.coerceAtLeast(0)) } },
                max = vm.pagerState.pageCount,
            )
        }
    }
}
