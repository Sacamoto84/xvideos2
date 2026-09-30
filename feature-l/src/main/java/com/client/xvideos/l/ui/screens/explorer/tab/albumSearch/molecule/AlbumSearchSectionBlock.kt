package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.atom.AlbumSearchGridItem

@Composable
fun AlbumSearchSectionBlock(
    section: Landing_page_albumSection,
    screenWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (Landing_page_albumSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleSeeAll = remember(section, onSeeAllClick) { { onSeeAllClick(section) } }
    val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }
    val seeAllShape = remember { RoundedCornerShape(8.dp) }

    Column(modifier = modifier) {
        Text(
            section.title,
            color = Theme.L.textColor,
            fontSize = 24.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
        )

        FlowRow(
            maxItemsInEachRow = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val albums = remember(section.items) { section.items.take(9) }
            albums.forEach { album ->
                key(album.id) {
                    AlbumSearchGridItem(
                        album = album,
                        itemWidth = itemWidth,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .height(40.dp)
                .border(2.dp, Theme.L.grey3, seeAllShape)
                .clickable(onClick = handleSeeAll),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "See All >",
                color = Theme.L.textColor,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontFamily = Theme.L.fontFamilyKarla,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview
@Composable
private fun AlbumSearchSectionBlockPreview() {
    AlbumSearchSectionBlock(
        section = Landing_page_albumSection(
            title = "Manga",
            count = 0,
            itemType = "album",
            url = "",
            items = emptyList()
        ),
        screenWidth = 360.dp,
        onAlbumClick = {},
        onSeeAllClick = {}
    )
}
