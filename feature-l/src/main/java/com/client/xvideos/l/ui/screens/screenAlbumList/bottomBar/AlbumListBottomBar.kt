package com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.atom.AlbumListPageSelector

private val filterButtonShape = RoundedCornerShape(4.dp)
private val filterButtonBorderColor = Color(0xFF434343)
private val filterButtonBgColor = Color(0xFF414141)

@Composable
fun AlbumListBottomBar(
    onClickVisibleFilter: () -> Unit,
    currentPage: Int,
    totalPages: Int,
    onChange: (Int) -> Unit = {}
) {
    Column {
        HorizontalDivider()

        Row(
            modifier = Modifier
                .padding(start = 0.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(Theme.tabLevel1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterButton(
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

            FilterButton(
                modifier = Modifier.padding(start = 2.dp),
                onClick = onClickVisibleFilter
            )
        }

        HorizontalDivider()
    }
}

@Composable
private fun FilterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(48.dp)
            .border(2.dp, filterButtonBorderColor, filterButtonShape)
            .background(filterButtonBgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White)
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
