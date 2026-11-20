package com.client.xvideos.n.ui.screens.explorer.tab.saved.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
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

private val ALL_SAVED_TAB_ICONS = persistentListOf(
    Icons.Outlined.FavoriteBorder,
    Icons.Outlined.Folder,
    Icons.Outlined.Apps,
    Icons.Outlined.Bookmark,
)

private val ALL_SAVED_TAB_HINTS = persistentListOf(
    "Лайки страниц",
    "Сохранённая манга",
    "Коллекции",
    "Избранное на сервере",
)

/**
 * Нижняя панель навигации по разделам Saved в модуле N:
 * 0: Лайки страниц
 * 1: Сохранённая манга (альбомы)
 * 2: Пользовательские коллекции
 * 3: Избранное на сервере (при авторизации)
 */
@Composable
fun N_SavedBottomBar(
    currentPage: Int,
    columnLikes: Int,
    columnManga: Int,
    columnCollection: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabCount: Int = 3
) {
    val renderOverlay: @Composable (Int) -> Unit = remember(columnLikes, columnManga, columnCollection, currentPage) {
        { index ->
            when (index) {
                0 -> TabBarPoints(columnLikes, currentPage == index)
                1, 3 -> TabBarPoints(columnManga, currentPage == index)
                2 -> TabBarPoints(columnCollection, currentPage == index)
            }
        }
    }

    val visibleIcons = remember(tabCount) { ALL_SAVED_TAB_ICONS.take(tabCount).toPersistentList() }
    val visibleHints = remember(tabCount) { ALL_SAVED_TAB_HINTS.take(tabCount).toPersistentList() }

    Column(modifier = modifier) {
        HorizontalDivider()
        TabRow(
            value = currentPage,
            containerColor = Theme.tabLevel1,
            titlesIcon = visibleIcons,
            onChangeState = onTabChange,
            overlay = renderOverlay,
            tooltips = visibleHints,
        )
    }
}

@Preview
@Composable
private fun N_SavedBottomBarPreview() {
    N_SavedBottomBar(
        currentPage = 0,
        columnLikes = 2,
        columnManga = 2,
        columnCollection = 2,
        tabCount = 4,
        onTabChange = {}
    )
}
