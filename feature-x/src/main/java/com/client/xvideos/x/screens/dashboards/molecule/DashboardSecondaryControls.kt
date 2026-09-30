package com.client.xvideos.x.screens.dashboards.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.dashboards.bottomBar.DashboardControlsRow
import com.client.xvideos.x.screens.search.SearchUiMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

const val MAIN_TAB_DASHBOARDS = 0
const val MAIN_TAB_SAVABLE = 1
const val MAIN_TAB_SEARCH = 2

private val savedTabs: ImmutableList<ImageVector> = persistentListOf(
    Icons.Outlined.FavoriteBorder,
    Icons.Outlined.Save,
    Icons.Outlined.History,
    Icons.Outlined.Tv,
    Icons.Outlined.Person,
)

@Composable
fun DashboardSecondaryControls(
    mainTab: Int,
    savedTab: Int,
    searchUiMode: SearchUiMode,
    searchPage: Int,
    searchMaxPages: Int,
    pagerCurrentPage: Int,
    pagerPageCount: Int,
    onSavedTabChange: (Int) -> Unit,
    onDashboardPageChange: suspend (Int) -> Unit,
    onSearchPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (mainTab) {
        MAIN_TAB_SAVABLE -> TabRow(
            titlesIcon = savedTabs,
            value = savedTab,
            onChangeState = onSavedTabChange,
            containerColor = Theme.tabLevel1,
        )
        MAIN_TAB_SEARCH -> {
            if (searchUiMode == SearchUiMode.RESULTS) {
                BottomListDashBoardNavigationButtons2(
                    value = searchPage,
                    onChange = onSearchPageChange,
                    max = searchMaxPages,
                    modifier = modifier,
                )
            }
        }
        else -> DashboardControlsRow(
            isCurrentPage = pagerCurrentPage,
            isMax = pagerPageCount,
            onChange = onDashboardPageChange,
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardSecondaryControlsPreview() {
    DashboardSecondaryControls(
        mainTab = MAIN_TAB_DASHBOARDS,
        savedTab = 0,
        searchUiMode = SearchUiMode.RESULTS,
        searchPage = 0,
        searchMaxPages = 10,
        pagerCurrentPage = 0,
        pagerPageCount = 10,
        onSavedTabChange = {},
        onDashboardPageChange = {},
        onSearchPageChange = {}
    )
}
