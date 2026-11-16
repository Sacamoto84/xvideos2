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
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.ProvidePagerScrollbarAlpha
import com.client.xvideos.n.ui.molecule.N_SavedBottomBar
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.N_ScreenSavedCollectionsTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.N_ScreenSavedCollectionsTab_AddColumn
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.N_ScreenSavedLikesTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.N_ScreenSavedLikesTab_AddColumn
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.N_ScreenSavedMangaTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.N_ScreenSavedMangaTab_AddColumn
import kotlinx.coroutines.launch

private const val N_SAVED_PAGE_COUNT = 3

/**
 * Корневая вкладка сохранённого раздела N (манга).
 * Содержит три подвкладки:
 * 0: Лайки отдельных страниц/картинок
 * 1: Сохранённые произведения (манга) целиком
 * 2: Пользовательские коллекции страниц
 */
object N_SavedTab : Screen {

    private fun readResolve(): Any = N_SavedTab

    override val key: ScreenKey = "N_SavedTab"

    @Composable
    override fun Content() {
        val vm = getScreenModel<N_SavedTabSM>()
        val scope = rememberCoroutineScope()

        val pagerState = rememberPagerState(
            initialPage = vm.screenType.coerceIn(0, N_SAVED_PAGE_COUNT - 1),
            pageCount = { N_SAVED_PAGE_COUNT }
        )

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
                        0 -> N_ScreenSavedLikesTab_AddColumn()
                        1 -> N_ScreenSavedMangaTab_AddColumn()
                        2 -> N_ScreenSavedCollectionsTab_AddColumn()
                    }
                } else {
                    scope.launch { pagerState.scrollToPage(tab) }
                }
            }
        }

        N_SavedTabContent(
            pagerState = pagerState,
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
                currentPage = pagerState.currentPage,
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
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun N_SavedTabContentPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { N_SAVED_PAGE_COUNT }
    N_SavedTabContent(
        pagerState = pagerState,
        columnLikes = 2,
        columnManga = 2,
        columnCollection = 2,
        isInsideCollection = false,
        onTabChange = {}
    )
}
