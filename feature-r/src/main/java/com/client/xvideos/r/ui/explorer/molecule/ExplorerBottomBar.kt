package com.client.xvideos.r.ui.explorer.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.common.ui.atom.TabBarPoints
import kotlinx.collections.immutable.persistentListOf

private val EXPLORER_TAB_ICONS = persistentListOf(
    Icons.Outlined.Movie,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Group,
    Icons.Outlined.Search
)

@Composable
fun ExplorerBottomBar(
    screenType: Int,
    overlay0: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderOverlay0: @Composable () -> Unit = remember(overlay0, screenType) {
        { TabBarPoints(overlay0, screenType == 0) }
    }

    Box(modifier = modifier) {
        TabRow(
            containerColor = Theme.tabLevel0,
            titlesIcon = EXPLORER_TAB_ICONS,
            value = screenType,
            onChangeState = onTabChange,
            overlay0 = renderOverlay0,
        )
    }
}

@Preview
@Composable
private fun ExplorerBottomBarPreview() {
    ExplorerBottomBar(
        screenType = 0,
        overlay0 = 2,
        onTabChange = {}
    )
}
