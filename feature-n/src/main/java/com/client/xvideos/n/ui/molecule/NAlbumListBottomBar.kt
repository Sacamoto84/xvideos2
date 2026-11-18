package com.client.xvideos.n.ui.molecule

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
import com.client.xvideos.n.ui.atom.NAlbumListPageSelector
import com.client.xvideos.n.ui.atom.NFilterButton

/**
 * Нижняя панель пагинации страниц каталога манги с поддержкой кнопки фильтров (как в разделе L).
 */
@Composable
fun NAlbumListBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null,
    hasActiveFilter: Boolean = false
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
            if (onFilterClick != null) {
                NFilterButton(
                    modifier = Modifier.padding(end = 2.dp),
                    onClick = onFilterClick,
                    hasActiveFilter = hasActiveFilter
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NAlbumListPageSelector(
                    page = currentPage,
                    pageMax = totalPages,
                    onChange = onPageChange
                )
            }

            if (onFilterClick != null) {
                NFilterButton(
                    modifier = Modifier.padding(start = 2.dp),
                    onClick = onFilterClick,
                    hasActiveFilter = hasActiveFilter
                )
            }
        }
        HorizontalDivider()
    }
}

@Preview
@Composable
private fun NAlbumListBottomBarPreview() {
    NAlbumListBottomBar(
        currentPage = 1,
        totalPages = 25,
        onPageChange = {},
        onFilterClick = {},
        hasActiveFilter = true
    )
}
