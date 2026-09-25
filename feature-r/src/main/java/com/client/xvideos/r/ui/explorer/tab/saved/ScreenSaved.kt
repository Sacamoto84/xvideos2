package com.client.xvideos.r.ui.explorer.tab.saved

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CollectionTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CreatorsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_DownloadTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_LikesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.SavedNichesTab
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_SubscriptionsTab
import com.client.xvideos.common.ui.atom.TabBarPoints
import com.client.xvideos.common.ui.atom.ProvidePagerScrollbarAlpha
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import kotlinx.collections.immutable.persistentListOf

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.r.ui.explorer.RNavigationState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object R_ScreenSavedTab : Screen {

    private fun readResolve(): Any = R_ScreenSavedTab

    override val key: ScreenKey = "R_ScreenSavedTab"

    private val SAVED_TAB_ICONS = persistentListOf(
        Icons.Outlined.FavoriteBorder,
        Icons.Outlined.Person,
        Icons.Outlined.Group,
        Icons.Outlined.Save,
        Icons.Outlined.Apps,
        Icons.Outlined.Subscriptions,
    )

    private val SAVED_CONTENT_INSETS = WindowInsets(0, 0, 0, 0)
    private val SAVED_PAGE_KEY_PROVIDER: (Int) -> Any = { page -> page }
    private val SAVED_PAGE_COUNT_PROVIDER: () -> Int = { 6 }
    private val FULL_SIZE_MODIFIER = Modifier.fillMaxSize()

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

        val overlay0Content: @Composable () -> Unit = remember(overlay0, pagerState.currentPage) {
            { TabBarPoints(overlay0, pagerState.currentPage == 0) }
        }
        val overlay4Content: @Composable () -> Unit = remember(overlay4, pagerState.currentPage) {
            { TabBarPoints(overlay4, pagerState.currentPage == 4) }
        }

        Scaffold(
            contentWindowInsets = SAVED_CONTENT_INSETS,
            bottomBar = {
                Column {
                    HorizontalDivider()
                    TabRow(
                        value = pagerState.currentPage,
                        containerColor = Theme.tabLevel1,
                        titlesIcon = SAVED_TAB_ICONS,
                        onChangeState = onTabChange,
                        overlay0 = overlay0Content,
                        overlay4 = overlay4Content,
                    )
                }
            },

            modifier = FULL_SIZE_MODIFIER,
            containerColor = Theme.background
        ) { paddingValues ->

            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                ProvidePagerScrollbarAlpha(pagerState = pagerState) {
                    HorizontalPager(
                        state = pagerState,
                        key = SAVED_PAGE_KEY_PROVIDER,
                        userScrollEnabled = !isInsideCollection,
                        beyondViewportPageCount = 0,
                        modifier = FULL_SIZE_MODIFIER
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
}

@Stable
class R_SavedTabSM @Inject constructor(
    private val navigationState: RNavigationState,
    val savedRed: SavedRed
) : ScreenModel {
    var screenType: Int
        get() = navigationState.savedTab
        set(value) {
            navigationState.savedTab = value
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class R_SavedTabModule {
    @Binds
    @IntoMap
    @ScreenModelKey(R_SavedTabSM::class)
    abstract fun bindR_SavedTabSM(sm: R_SavedTabSM): ScreenModel
}
