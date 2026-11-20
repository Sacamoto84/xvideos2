package com.client.xvideos.n.ui.screens.explorer.tab.saved

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.ProvidePagerScrollbarAlpha
import com.client.xvideos.n.net.NAuthState
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.N_ScreenSavedCollectionsTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.N_ScreenSavedLikesTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.N_ScreenSavedMangaTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.molecule.N_SavedBottomBar
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.N_ScreenServerFavoritesTab
import kotlinx.coroutines.launch

/**
 * Корневая вкладка сохранённого раздела N (манга).
 * Содержит локальные и серверные подвкладки:
 * 0: Лайки отдельных страниц/картинок
 * 1: Сохранённые произведения (манга) целиком локально
 * 2: Пользовательские коллекции страниц
 * 3: Избранное на сервере (только при авторизации)
 */
object N_SavedTab : Screen {

    private fun readResolve(): Any = N_SavedTab

    override val key: ScreenKey = "N_SavedTab"

    @Composable
    override fun Content() {
        val vm = getScreenModel<N_SavedTabSM>()
        val scope = rememberCoroutineScope()

        val isLoggedIn by NAuthState.isLoggedIn.collectAsStateWithLifecycle()
        val pageCount = nSavedTabPageCount(isLoggedIn)

        val pagerState = rememberPagerState(
            initialPage = nSavedTabPage(vm.screenType, pageCount),
            pageCount = { pageCount }
        )

        LaunchedEffect(pageCount) {
            val visible = nSavedTabPage(pagerState.currentPage, pageCount)
            if (visible != pagerState.currentPage) {
                pagerState.scrollToPage(visible)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            vm.screenType = pagerState.currentPage
        }

        val selectedCollection = vm.saved.collection.currentCollectionName
        val isInsideCollection = pagerState.currentPage == 2 && selectedCollection != null

        val onBackToFirstPage: () -> Unit = remember(scope, pagerState) {
            {
                scope.launch { pagerState.scrollToPage(0) }.let {}
            }
        }

        BackHandler(
            enabled = pagerState.currentPage != 0 && !isInsideCollection,
            onBack = onBackToFirstPage
        )

        val columnLikes by Settings.n_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnManga by Settings.n_saved_column_current_count.field.collectAsStateWithLifecycle()
        val columnCollection by Settings.n_collectionTab_column_current_count.field.collectAsStateWithLifecycle()

        val onTabChange: (Int) -> Unit = remember(pagerState, scope) {
            { tab ->
                if (tab == pagerState.currentPage) {
                    when (tab) {
                        0 -> ColumnSelect_AddColumn(Settings.n_likesTab_column_current_count, Settings.n_likesTab_G_0_4)
                        1, 3 -> ColumnSelect_AddColumn(Settings.n_saved_column_current_count, Settings.n_galleriesTab_G_0_4)
                        2 -> ColumnSelect_AddColumn(Settings.n_collectionTab_column_current_count, Settings.n_collectionTab_G_0_4)
                    }
                } else {
                    scope.launch { pagerState.scrollToPage(tab) }
                }
            }
        }

        N_SavedTabContent(
            pagerState = pagerState,
            pageCount = pageCount,
            columnLikes = columnLikes,
            columnManga = columnManga,
            columnCollection = columnCollection,
            isInsideCollection = isInsideCollection,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun N_SavedTabContent(
    pagerState: PagerState,
    pageCount: Int,
    columnLikes: Int,
    columnManga: Int,
    columnCollection: Int,
    isInsideCollection: Boolean,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            N_SavedBottomBar(
                currentPage = nSavedTabPage(pagerState.currentPage, pageCount),
                tabCount = pageCount,
                columnLikes = columnLikes,
                columnManga = columnManga,
                columnCollection = columnCollection,
                onTabChange = onTabChange
            )
        },
        modifier = modifier.fillMaxSize(),
        containerColor = Theme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            ProvidePagerScrollbarAlpha(pagerState = pagerState) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isInsideCollection,
                    beyondViewportPageCount = 0,
                    key = { page -> page },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> N_ScreenSavedLikesTab.Content()
                        1 -> N_ScreenSavedMangaTab.Content()
                        2 -> N_ScreenSavedCollectionsTab.Content()
                        3 -> N_ScreenServerFavoritesTab.Content(active = pagerState.currentPage == 3)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun N_SavedTabContentPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    N_SavedTabContent(
        pagerState = pagerState,
        pageCount = 4,
        columnLikes = 2,
        columnManga = 2,
        columnCollection = 2,
        isInsideCollection = false,
        onTabChange = {}
    )
}
