package com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.AlbumListFilterButton
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.AlbumListPageSelector

@Composable
fun AlbumListBottomBar(
    onClickVisibleFilter: () -> Unit,
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit = {}
) {
    Column(modifier = modifier) {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Theme.tabLevel1)
                .adaptiveContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AlbumListFilterButton(
                modifier = Modifier.padding(end = 2.dp),
                onClick = onClickVisibleFilter
            )

            Box(modifier = Modifier.weight(1f)) {
                AlbumListPageSelector(
                    page = currentPage,
                    pageMax = totalPages,
                    onChange = onChange
                )
            }

            AlbumListFilterButton(
                modifier = Modifier.padding(start = 2.dp),
                onClick = onClickVisibleFilter
            )
        }

        HorizontalDivider()
    }
}

@Preview
@Composable
fun AlbumListBottomBarPreview() {
    AlbumListBottomBar(
        onClickVisibleFilter = {},
        currentPage = 1,
        totalPages = 10,
        onChange = {}
    )
}
