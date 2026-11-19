package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.common.ui.atom.TabBarPoints
import kotlinx.collections.immutable.persistentListOf

private val EXPLORER_ICONS = persistentListOf(
    Icons.AutoMirrored.Outlined.FormatListBulleted,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Search,
)

private val EXPLORER_HINTS = persistentListOf(
    "Манга",
    "Сохранённое",
    "Поиск",
)

private val EXPLORER_TAGS = persistentListOf(
    "tabGalleries",
    "tabSaved",
    "tabSearch",
)

/**
 * Нижняя панель навигации раздела nhentai.
 */
@Composable
fun N_ExplorerBottomBar(
    screenType: Int,
    percentDownload: Float,
    columnCount: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderOverlay: @Composable (Int) -> Unit = remember(columnCount, screenType) {
        { index -> if (index == 0) TabBarPoints(columnCount, screenType == 0) }
    }

    Column(modifier = modifier) {
        DownloadIndicator(percentDownload)
        TabRow(
            containerColor = Theme.tabLevel0,
            titlesIcon = EXPLORER_ICONS,
            value = screenType,
            onChangeState = onTabChange,
            overlay = renderOverlay,
            tags = EXPLORER_TAGS,
            tooltips = EXPLORER_HINTS,
        )
    }
}

@Preview
@Composable
private fun N_ExplorerBottomBarPreview() {
    N_ExplorerBottomBar(
        screenType = 0,
        percentDownload = 0f,
        columnCount = 2,
        onTabChange = {}
    )
}
