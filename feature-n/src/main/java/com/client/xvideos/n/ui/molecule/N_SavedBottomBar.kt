package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
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

private val SAVED_TAB_ICONS = persistentListOf(
    Icons.Outlined.FavoriteBorder,
    Icons.Outlined.Folder,
    Icons.Outlined.Apps,
)

private val SAVED_TAB_HINTS = persistentListOf(
    "Лайки страниц",
    "Сохранённая манга",
    "Коллекции",
)

/**
 * Нижняя панель навигации по разделам Saved в модуле N:
 * 0: Лайки страниц
 * 1: Сохранённая манга (альбомы)
 * 2: Пользовательские коллекции
 */
@Composable
fun N_SavedBottomBar(
    currentPage: Int,
    columnLikes: Int,
    columnManga: Int,
    columnCollection: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderOverlay: @Composable (Int) -> Unit = remember(columnLikes, columnManga, columnCollection, currentPage) {
        { index ->
            when (index) {
                0 -> TabBarPoints(columnLikes, currentPage == index)
                1 -> TabBarPoints(columnManga, currentPage == index)
                2 -> TabBarPoints(columnCollection, currentPage == index)
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalDivider()
        TabRow(
            value = currentPage,
            containerColor = Theme.tabLevel1,
            titlesIcon = SAVED_TAB_ICONS,
            onChangeState = onTabChange,
            overlay = renderOverlay,
            tooltips = SAVED_TAB_HINTS,
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
        onTabChange = {}
    )
}
