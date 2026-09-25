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
import androidx.compose.foundation.layout.size
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

private val FILTER_BUTTON_SHAPE = RoundedCornerShape(4.dp)
private val FILTER_BUTTON_BORDER_COLOR = Color(0xFF434343)
private val FILTER_BUTTON_BG_COLOR = Color(0xFF414141)
private val FILTER_BUTTON_SIZE = 48.dp
private val FILTER_BUTTON_BORDER_WIDTH = 2.dp
private val ROW_BAR_HEIGHT = 48.dp
private val FILTER_START_PADDING = 2.dp
private val FILTER_END_PADDING = 2.dp
private const val CD_FILTER_BUTTON = "Фильтры"

private val FILTER_BUTTON_BASE_MODIFIER = Modifier
    .size(FILTER_BUTTON_SIZE)
    .border(FILTER_BUTTON_BORDER_WIDTH, FILTER_BUTTON_BORDER_COLOR, FILTER_BUTTON_SHAPE)
    .background(FILTER_BUTTON_BG_COLOR)

private val FILTER_BUTTON_START_MODIFIER = Modifier.padding(start = FILTER_START_PADDING)
private val FILTER_BUTTON_END_MODIFIER = Modifier.padding(end = FILTER_END_PADDING)
private val ROW_BAR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .height(ROW_BAR_HEIGHT)

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
            modifier = ROW_BAR_BASE_MODIFIER.background(Theme.tabLevel1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterButton(
                modifier = FILTER_BUTTON_END_MODIFIER,
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
                modifier = FILTER_BUTTON_START_MODIFIER,
                onClick = onClickVisibleFilter
            )
        }

        HorizontalDivider()
    }
}

@Composable
private fun FilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .then(FILTER_BUTTON_BASE_MODIFIER)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.FilterList, contentDescription = CD_FILTER_BUTTON, tint = Color.White)
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

