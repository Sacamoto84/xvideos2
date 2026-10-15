package com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.common.ui.atom.TabBarPoints
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private val SAVED_TAB_ICONS = persistentListOf(
    Icons.Outlined.Save,
    Icons.Outlined.Folder,
    Icons.Outlined.Apps,
    Icons.Outlined.Subscriptions,
    Icons.Outlined.FavoriteBorder,
)

@Composable
fun L_SavedBottomBar(
    currentPage: Int,
    tabCount: Int,
    columnLikes: Int,
    columnCollection: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPage0 = currentPage == 0
    val isPage2 = currentPage == 2
    val isPage4 = currentPage == 4
    val overlay0Composable: @Composable () -> Unit = remember(columnLikes, isPage0) {
        { TabBarPoints(columnLikes, isPage0) }
    }
    val overlay2Composable: @Composable () -> Unit = remember(columnCollection, isPage2) {
        { TabBarPoints(columnCollection, isPage2) }
    }
    val overlay4Composable: @Composable () -> Unit = remember(columnLikes, isPage4) {
        { TabBarPoints(columnLikes, isPage4) }
    }

    // Без аккаунта L серверных вкладок нет: показываются только первые tabCount.
    val icons = remember(tabCount) { SAVED_TAB_ICONS.take(tabCount).toPersistentList() }

    Column(modifier = modifier) {
        HorizontalDivider()
        TabRow(
            value = currentPage,
            containerColor = Theme.tabLevel1,
            titlesIcon = icons,
            onChangeState = onTabChange,
            overlay0 = overlay0Composable,
            overlay2 = overlay2Composable,
            overlay4 = overlay4Composable
        )
    }
}

@Preview
@Composable
private fun L_SavedBottomBarPreview() {
    L_SavedBottomBar(
        currentPage = 0,
        tabCount = 5,
        columnLikes = 2,
        columnCollection = 2,
        onTabChange = {}
    )
}
