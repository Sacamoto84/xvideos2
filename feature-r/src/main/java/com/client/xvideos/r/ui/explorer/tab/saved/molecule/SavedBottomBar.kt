package com.client.xvideos.r.ui.explorer.tab.saved.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
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

private val SAVED_TAB_ICONS = persistentListOf(
    Icons.Outlined.FavoriteBorder,
    Icons.Outlined.Person,
    Icons.Outlined.Group,
    Icons.Outlined.Save,
    Icons.Outlined.Apps,
    Icons.Outlined.Subscriptions,
)

@Composable
fun SavedBottomBar(
    currentPage: Int,
    overlay0: Int,
    overlay4: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderOverlay: @Composable (Int) -> Unit = remember(overlay0, overlay4, currentPage) {
        { index ->
            when (index) {
                0 -> TabBarPoints(overlay0, currentPage == 0)
                4 -> TabBarPoints(overlay4, currentPage == 4)
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
        )
    }
}

@Preview
@Composable
private fun SavedBottomBarPreview() {
    SavedBottomBar(
        currentPage = 0,
        overlay0 = 2,
        overlay4 = 2,
        onTabChange = {}
    )
}
