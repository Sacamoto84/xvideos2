package com.client.xvideos.l.ui.screens.explorer.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Topic
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
    Icons.Outlined.Topic,
    Icons.Outlined.Search,
)

private val EXPLORER_TAGS = persistentListOf(
    "",
    "",
    "bBookMark",
    ""
)

@Composable
fun L_ExplorerBottomBar(
    screenType: Int,
    percentDownload: Float,
    columnGifsTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderOverlay0: @Composable () -> Unit = remember(columnGifsTab, screenType) {
        { TabBarPoints(columnGifsTab, screenType == 0) }
    }

    Column(modifier = modifier) {
        DownloadIndicator(percentDownload)
        TabRow(
            containerColor = Theme.tabLevel0,
            titlesIcon = EXPLORER_ICONS,
            value = screenType,
            onChangeState = onTabChange,
            overlay0 = renderOverlay0,
            tags = EXPLORER_TAGS
        )
    }
}

@Preview
@Composable
private fun L_ExplorerBottomBarPreview() {
    L_ExplorerBottomBar(
        screenType = 0,
        percentDownload = 0f,
        columnGifsTab = 1,
        onTabChange = {}
    )
}
