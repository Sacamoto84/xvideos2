package com.client.xvideos.r.ui.explorer.tab.saved

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
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.saved.molecule.SavedBottomBar
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CollectionTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CreatorsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_DownloadTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_LikesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_SubscriptionsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.SavedNichesTab
import kotlinx.coroutines.launch

object R_ScreenSavedTab : Screen {

    private fun readResolve(): Any = R_ScreenSavedTab

    override val key: ScreenKey = "R_ScreenSavedTab"

    private val SAVED_PAGE_COUNT_PROVIDER: () -> Int = { 6 }

    @Composable
    override fun Content() {
        val vm = getScreenModel<R_SavedTabSM>()
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = vm.screenType.coerceIn(0, 5),
            pageCount = SAVED_PAGE_COUNT_PROVIDER
        )

        LaunchedEffect(pagerState.currentPage) {
            vm.screenType = pagerState.currentPage
        }

        val selectedCollection by vm.savedRed.collections.selectedCollection.collectAsStateWithLifecycle()
        val isInsideCollection = pagerState.currentPage == 4 && selectedCollection != null

        BackHandler(enabled = pagerState.currentPage != 0 && !isInsideCollection) {
            scope.launch { pagerState.scrollToPage(0) }
        }

        val overlay0Raw by Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        val overlay0 = normalizeRColumnCount(overlay0Raw)

        val overlay4Raw by Settings.r_collectionTab_column_current_count.field.collectAsStateWithLifecycle()
        val overlay4 = normalizeRColumnCount(overlay4Raw)

        val onTabChange: (Int) -> Unit = remember(pagerState, scope) {
            { tab ->
                if (tab == pagerState.currentPage) {
                    when (tab) {
                        0 -> { ColumnSelect_AddRColumn(Settings.r_likesTab_column_current_count) }
                        4 -> { ColumnSelect_AddRColumn(Settings.r_collectionTab_column_current_count) }
                    }
                } else {
                    scope.launch { pagerState.scrollToPage(tab) }
                }
            }
        }

        R_ScreenSavedTabContent(
            pagerState = pagerState,
            overlay0 = overlay0,
            overlay4 = overlay4,
            isInsideCollection = isInsideCollection,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun R_ScreenSavedTabContent(
    pagerState: PagerState,
    overlay0: Int,
    overlay4: Int,
    isInsideCollection: Boolean,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            SavedBottomBar(
                currentPage = pagerState.currentPage,
                overlay0 = overlay0,
                overlay4 = overlay4,
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
                    key = { page -> page },
                    userScrollEnabled = !isInsideCollection,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> R_Screen_Saved_LikesTab.Content()
                        1 -> R_Screen_CreatorsTab.Content()
                        2 -> SavedNichesTab.Content()
                        3 -> R_Screen_Saved_DownloadTab.Content()
                        4 -> R_Screen_CollectionTab.Content()
                        5 -> R_Screen_Saved_SubscriptionsTab.Content()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun R_ScreenSavedTabPreview() {
    val pagerState = rememberPagerState(initialPage = 0) { 6 }
    R_ScreenSavedTabContent(
        pagerState = pagerState,
        overlay0 = 2,
        overlay4 = 2,
        isInsideCollection = false,
        onTabChange = {}
    )
}
