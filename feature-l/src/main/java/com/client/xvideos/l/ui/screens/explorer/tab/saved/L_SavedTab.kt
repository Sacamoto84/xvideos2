package com.client.xvideos.l.ui.screens.explorer.tab.saved

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
import com.client.xvideos.l.ui.screens.explorer.tab.saved.albums.L_ScreenSavedAlbumsTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.L_Screen_CollectionTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.L_ScreenSavedLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.L_ScreenSavedLikesTab_AddColumn
import com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule.L_SavedBottomBar
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.L_ScreenServerLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.L_ScreenSubscribedAlbumsTab
import kotlinx.coroutines.launch

object L_SavedTab : Screen {

    private fun readResolve(): Any = L_SavedTab

    override val key: ScreenKey = "L_SavedTab"

    @Composable
    override fun Content() {
        val vm = getScreenModel<L_SavedTabSM>()
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = vm.screenType.coerceIn(0, 4),
            pageCount = { 5 }
        )

        LaunchedEffect(pagerState.currentPage) {
            vm.screenType = pagerState.currentPage
        }

        val selectedCollection = vm.savedL.collection.currentCollectionName
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

        val columnLikes by Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnCollection by Settings.l_collectionTab_column_current_count.field.collectAsStateWithLifecycle()

        val onTabChange: (Int) -> Unit = remember(pagerState, scope) {
            { tab ->
                if (tab == pagerState.currentPage) {
                    when (tab) {
                        0 -> L_ScreenSavedLikesTab_AddColumn()
                        2 -> { ColumnSelect_AddColumn(Settings.l_collectionTab_column_current_count, Settings.l_collectionTab_G_0_4) }
                        4 -> L_ScreenSavedLikesTab_AddColumn()
                    }
                } else {
                    scope.launch { pagerState.scrollToPage(tab) }
                }
            }
        }

        L_SavedTabContent(
            pagerState = pagerState,
            columnLikes = columnLikes,
            columnCollection = columnCollection,
            isInsideCollection = isInsideCollection,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun L_SavedTabContent(
    pagerState: PagerState,
    columnLikes: Int,
    columnCollection: Int,
    isInsideCollection: Boolean,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            L_SavedBottomBar(
                currentPage = pagerState.currentPage,
                columnLikes = columnLikes,
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
                        0 -> L_ScreenSavedLikesTab.Content()
                        1 -> L_ScreenSavedAlbumsTab.Content()
                        2 -> L_Screen_CollectionTab.Content()
                        3 -> L_ScreenSubscribedAlbumsTab.Content()
                        4 -> L_ScreenServerLikesTab.Content()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun L_SavedTabContentPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { 5 }
    L_SavedTabContent(
        pagerState = pagerState,
        columnLikes = 2,
        columnCollection = 2,
        isInsideCollection = false,
        onTabChange = {}
    )
}
